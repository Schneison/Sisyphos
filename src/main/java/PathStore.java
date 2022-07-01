import robot.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

/**
 * Center collection which can be used to get the paths to a material on the field or from a material to the factory and
 * vise versa.
 */
public class PathStore {
    private final Map<Point, Path> factories;
    private final Map<Point, List<Path>> neighbors;
    private final Map<PointPair, Path> allPaths;
    private final TimeLookup timeLookup;
    private final PathCreator creator;
    private final World world;

    public PathStore(Environment env, Config config) {
        this.world = env.getWorld();
        this.creator = env.getCreator();
        this.factories = new HashMap<>();
        this.neighbors = new HashMap<>();
        this.allPaths = new HashMap<>();

        List<Path> pathsToMaterial = creator.findPathsToMaterial();
        Map<Point, Path> materialPaths = pathsToMaterial.stream().collect(Collectors.toMap(Path::getDestinationPos, Function.identity()));
        this.timeLookup = new TimeLookup(pathsToMaterial.size());
        for (int x = 0; x < world.getN(); x++) {
            for (int y = 0; y < world.getN(); y++) {
                if (world.getFieldMaterials(x, y) > 0) {
                    Point pos = new Point(x, y);
                    Path deliveryPath = materialPaths.get(pos).invert();
                    List<Path> validNeighbors = creator.createPaths(
                            pos, p -> p.hasMaterials(world) && !p.at(pos),
                            config.getNeighborLimit(),
                            config.getDistanceLimit(world.getN())
                    );
                    for (Path p : validNeighbors) {
                        Path inverted = p.invert();
                        timeLookup.setEdge(p.getDestinationPos(), pos, inverted.getTimeCost());
                        timeLookup.setEdge(pos, p.getDestinationPos(), p.getTimeCost());
                        allPaths.putIfAbsent(new PointPair(p.getDestinationPos(), pos), inverted);
                        allPaths.putIfAbsent(new PointPair(pos, p.getDestinationPos()), p);
                    }
                    timeLookup.setFactory(pos, deliveryPath.getTimeCost());
                    neighbors.put(pos, validNeighbors);
                    factories.put(pos, deliveryPath);
                }
            }
        }
    }

    /**
     * Searches the path to the neighbors of the given positions.
     *
     * @param rang   All position to find the neighbors for
     * @param config Configuration of pathfinding
     */
    public Map<Point, List<Path>> searchNeighbors(Collection<Point> rang, Config config) {
        Map<Point, List<Path>> remainingNeighbors = new HashMap<>();
        for (Point pos : rang) {
            Set<Point> remaining = new HashSet<>(rang);
            remaining.remove(pos);
            List<Path> neighborPaths = new ArrayList<>();
            Iterator<Point> iterator = remaining.iterator();
            Point value;
            while (iterator.hasNext()) {
                value = iterator.next();
                Path neighbor = allPaths.get(new PointPair(pos, value));
                if (neighbor != null) {
                    neighborPaths.add(neighbor);
                    iterator.remove();
                }
            }
            remaining.add(pos);
            int limit = Math.min(config.getNeighborLimit(), remaining.size() - 1);
            neighborPaths.addAll(creator.createPaths(
                    pos, p -> p.hasMaterials(world) && !p.at(pos) && remaining.contains(p),
                    remaining,
                    limit,
                    config.getDistanceLimit(world.getN())
            ));
            for (Path p : neighborPaths) {
                Path inverted = p.invert();
                timeLookup.setEdge(p.getDestinationPos(), pos, inverted.getTimeCost());
                timeLookup.setEdge(pos, p.getDestinationPos(), p.getTimeCost());
                allPaths.putIfAbsent(new PointPair(p.getDestinationPos(), pos), inverted);
                allPaths.putIfAbsent(new PointPair(pos, p.getDestinationPos()), p);
            }
            remainingNeighbors.put(pos, neighborPaths);
        }
        return remainingNeighbors;
    }

    public World getWorld() {
        return world;
    }

    /**
     * Returns the path from the given position to the factory.
     */
    public Path getPathToFactory(Point p) {
        return factories.get(p);
    }

    /**
     * Returns the path from the given position to a certain amount of neighbors.
     * <p>The upper limit of this amount is defined by {@link Config#getNeighborLimit()}.
     */
    public List<Path> getNeighborPaths(Point p) {
        return neighbors.get(p);
    }

    /**
     * Returns a map with all cached paths to the neighbors of one position.
     */
    public Map<Point, List<Path>> getAllNeighbors() {
        return neighbors;
    }

    /**
     * Returns a map with all cached paths to the factory from one position.
     */
    public Map<Point, Path> getFactoryPaths() {
        return factories;
    }

    public TimeLookup getLookup() {
        return timeLookup;
    }

    /**
     * Helper class which can be used to set parameters of the current cycle of path finding.
     */
    public static final class Config {
        private final IntUnaryOperator distanceLimit;
        private final int neighbors;

        public Config(IntUnaryOperator distanceLimit, int neighbors) {
            this.distanceLimit = distanceLimit;
            this.neighbors = neighbors;
        }

        /**
         * Limits the distance from the origin to nodes that will be visited by the algorithm, used to minimize the time
         */
        public int getDistanceLimit(int n) {
            return distanceLimit.applyAsInt(n);
        }

        /**
         * Limits the amount of neighbors that are cached in the current cycle of the path finding.
         */
        public int getNeighborLimit() {
            return neighbors;
        }
    }

    /**
     * Helper class to simulate a double key.
     * This is used to cache the found paths.
     */
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
