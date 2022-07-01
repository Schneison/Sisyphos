import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class CompositorSeason {

    public static CompositorSeason fromStore(PathStore store) {
        return new CompositorSeason(
                store.getFactoryPaths(),
                store.getAllNeighbors()
        );
    }

    public static CompositorSeason fromSeason(CompositorSeason previous,
                                              PathStore store,
                                              PathStore.Config config
    ) {
        return new CompositorSeason(
                previous.remainingPaths,
                store.searchNeighbors(previous.remainingPaths.keySet(), config)
        );
    }

    public static CompositorSeason fromRange(PathStore store,
                                             Collection<Point> range,
                                             PathStore.Config config
    ) {
        Map<Point, Path> remaining = new HashMap<>();
        for (Point pos : range) {
            remaining.put(pos, store.getPathToFactory(pos));
        }
        return new CompositorSeason(
                remaining,
                store.searchNeighbors(range, config)
        );
    }

    private final Map<Point, Path> remainingPaths;
    private final Map<Point, List<Path>> remainingNeighbors;
    private final PriorityQueue<Path> bestPaths;

    private CompositorSeason(Map<Point, Path> remainingPaths, Map<Point, List<Path>> remainingNeighbors) {
        this.remainingPaths = new HashMap<>(remainingPaths);
        this.remainingNeighbors = new HashMap<>(remainingNeighbors);
        this.bestPaths = new PriorityQueue<>();
        this.bestPaths.addAll(remainingPaths.values());
    }

    public boolean hasRemaining() {
        return !remainingPaths.isEmpty();
    }

    public int getRemainingCount() {
        return remainingPaths.size();
    }

    public Path toFactory(Point p) {
        return remainingPaths.get(p);
    }

    public List<Path> getNeighbors(Point p) {
        return remainingNeighbors.get(p);
    }

    // TODO: sad
    public Path getPathTo(Point a, Point b) {
        for (Path p : getNeighbors(a)) {
            if (p.getDestinationPos().equals(b)) {
                return p;
            }
        }
        for (Path p : getNeighbors(b)) {
            if (p.getDestinationPos().equals(a)) {
                return p;
            }
        }
        throw new IllegalStateException();
    }

    public void usePositions(Point... points) {
        for (Point p : points) {
            if (p == null) {
                continue;
            }
            remainingPaths.remove(p);
            remainingNeighbors.remove(p);
        }
    }

    public PriorityQueue<Path> getBestPaths() {
        return bestPaths;
    }
}
