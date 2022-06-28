import robot.World;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

/**
 * A c
 */
public class PathStore {
    private final Map<Point, Path> factories;
    private final Map<Point, List<Path>> neighbors;
    private final Map<PointPair, Path> allPaths;
    private final TimeLookup timeLookup;
    private final World world;

    public PathStore(World world, PathCreator pathCreator, Config config) {
        this.world = world;
        this.factories = new HashMap<>();
        this.neighbors = new HashMap<>();
        this.allPaths = new HashMap<>();

        List<Path> pathsToMaterial = pathCreator.findPathsToMaterial();
        Map<Point, Path> materialPaths = pathsToMaterial.stream().collect(Collectors.toMap(Path::getDestinationPos, Function.identity()));
        this.timeLookup = new TimeLookup(pathsToMaterial.size());
        for (int x = 0; x < world.getN(); x++) {
            for (int y = 0; y < world.getN(); y++) {
                if (world.getFieldMaterials(x, y) > 0) {
                    Point pos = new Point(x, y);
                    Path deliveryPath = materialPaths.get(pos).invert();
                    List<Path> validNeighbors = pathCreator.createPaths(
                            pos, (p) -> p.hasMaterials(world) && !p.at(pos),
                            config.getNeighborLimit(),
                            config.getDistanceLimit(world.getN())
                    );
                    for (Path p : validNeighbors) {
                        timeLookup.setEdge(p.getDestinationPos(), pos, p.getTimeCost());
                        allPaths.putIfAbsent(new PointPair(p.getDestinationPos(), pos), p.invert());
                        allPaths.putIfAbsent(new PointPair(pos, p.getDestinationPos()), p);
                    }
                    timeLookup.setFactory(pos, deliveryPath.getTimeCost());
                    neighbors.put(pos, validNeighbors);
                    factories.put(pos, deliveryPath);
                }
            }
        }
    }

    public Map<Point, List<Path>> searchNeighbors(Map<Point, Path> remainingPaths, PathCreator pathCreator, Config config) {
        Map<Point, List<Path>> remainingNeighbors = new HashMap<>();
        for (Point pos : remainingPaths.keySet()) {
            Set<Point> remaining = new HashSet<>(remainingPaths.keySet());
            remaining.remove(pos);
            List<Path> neighbors = new ArrayList<>();
            Iterator<Point> iterator = remaining.iterator();
            Point value;
            while (iterator.hasNext()) {
                value = iterator.next();
                Path neighbor = allPaths.get(new PointPair(pos, value));
                if(neighbor != null) {
                    neighbors.add(neighbor);
                    iterator.remove();
                }
            }
            remaining.add(pos);
            int limit = Math.min(config.getNeighborLimit(), remaining.size() - 1);
            neighbors.addAll(pathCreator.createPaths(
                    pos, (p) -> p.hasMaterials(world) && !p.at(pos) && remaining.contains(p),
                    remaining,
                    limit,
                    config.getDistanceLimit(world.getN())
            ));
            for (Path p : neighbors) {
                timeLookup.setEdge(p.getDestinationPos(), pos, p.getTimeCost());
                allPaths.putIfAbsent(new PointPair(p.getDestinationPos(), pos), p.invert());
                allPaths.putIfAbsent(new PointPair(pos, p.getDestinationPos()), p);
            }
            remainingNeighbors.put(pos, neighbors);
        }
        return remainingNeighbors;
    }

    public Path getPathToFactory(Point p) {
        return factories.get(p);
    }

    public List<Path> getNeighborPaths(Point p) {
        return neighbors.get(p);
    }

    public Map<Point, List<Path>> getAllNeighbors() {
        return neighbors;
    }

    public Map<Point, Path> getFactoryPaths() {
        return factories;
    }

    public TimeLookup getLookup() {
        return timeLookup;
    }

    public static final class Config {
        private final IntUnaryOperator distanceLimit;
        private final int neighbors;

        public Config(IntUnaryOperator distanceLimit, int neighbors) {
            this.distanceLimit = distanceLimit;
            this.neighbors = neighbors;
        }

        public int getDistanceLimit(int n){
            return distanceLimit.applyAsInt(n);
        }

        public int getNeighborLimit(){
            return neighbors;
        }
    }

    private static final class PointPair {
        private final Point a;
        private final Point b;

        public PointPair(Point a, Point b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PointPair posPair)) return false;
            return a.equals(posPair.a) && b.equals(posPair.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }
    }
}
