import robot.World;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Main class for pathfinding. This class used the dijkstra algorithm to find the shortest path to one or more locations
 * on the field.
 * <p>
 * This is a very important dependency of this project. To take less time to find the many paths on the big field,
 * these paths on the nodes on the field are cached and reused to minimize the time that is needed to calculate these
 * paths.
 */
public class PathCreator {
    private final World world;
    public static int lastGeneration = 0;
    private final int costMultiplier;
    /**
     * Cached nodes that are used to calculate the cost of each step.
     */
    private final Node[][] cachedNodes;
    /**
     * Position of the factory
     */
    private final Point factoryPos;
    /**
     * Used so we don't have to allocate the memory every time
     */
    private final MutablePoint cachedPos;
    public int generation;

    public PathCreator(Environment env, Point factoryPos) {
        this.world = env.getWorld();
        this.costMultiplier = world.getN() * world.getN();
        this.cachedNodes = new Node[world.getN()][world.getN()];
        this.factoryPos = factoryPos;
        this.cachedPos = new MutablePoint();
    }

    private Node getOriginNode(Point point) {
        return getNode(point, null, false);
    }

    private Node getNode(Position point, Node root, boolean change) {
        Node cached = cachedNodes[point.getY()][point.getX()];
        if (cached == null) {
            int time = point.getTime(world);

            cached = new Node(point.toImmutable(), time, costMultiplier);
            cachedNodes[point.getY()][point.getX()] = cached;
        }
        // Was already called by other root this run, only update if new root is better
        if (change) {
            cached.tryUpdate(root);
        } else {
            cached.init(root);
        }
        return cached;
    }

//    public static final int CLOSED_FLAG = 1 << 0;
//    public static final int IN_QUEUE_FLAG = 1 << 1;
//    public static final int COUNT_VALUE = 1 << 2 | 1 << 3;
//
//    public boolean isClosed(byte value) {
//        return (value & CLOSED_FLAG) == CLOSED_FLAG;
//    }
//
//    public boolean isInQueue(byte value) {
//        return (value & IN_QUEUE_FLAG) == IN_QUEUE_FLAG;
//    }
//
//    public byte setClosed(byte value, boolean state) {
//        if (state) {
//            return (byte) (value | CLOSED_FLAG);
//        } else {
//            return (byte) (value & ~(CLOSED_FLAG));
//        }
//    }
//
//    public byte setInQueue(byte value, boolean state) {
//        if (state) {
//            return (byte) (value | IN_QUEUE_FLAG);
//        } else {
//            return (byte) (value & ~(IN_QUEUE_FLAG));
//        }
//    }
//
//    public int getCount(byte value) {
//        return (value & COUNT_VALUE) >> 2;
//    }
//
//    public byte setCount(byte value, byte state) {
//        value &= ~(COUNT_VALUE);
//        return (byte) (value | (state << 2) & COUNT_VALUE);
//    }

    /**
     * Tries to find the shortest path to one or more destinations using the dijkstra algorithm.
     *
     * @param <T>           The type of the returned type
     * @param startPoint    Start position of the graph
     * @param isDestination Tests if the given position is a valid destination.
     * @param validNeighbor Test if the given position is a valid neighbor node. Mostly used to limit the amount of nodes
     *                      which are visited per search.
     * @param consumePath   Called after a destination was found. The returned value will be returned by the function.
     *                      If null is returned, the search will continue and this method will be called again after the
     *                      next valid position was found.
     *                      If nothing ever gets returned the search will end if there are no more valid nodes to visit.
     * @param defaultValue  The value that will be returned if the search ends with no valid destination
     * @return The value returned by the consumePath function if one was supplied.
     */
    private  <T> T findPath(
            Position startPoint,
            Predicate<Point> isDestination,
            Predicate<Position> validNeighbor,
            Function<Path, T> consumePath,
            Supplier<T> defaultValue
    ) {
        PriorityQueue<Node> open = new PriorityQueue<>(world.getN() / 4);
        boolean[][] inQueue = new boolean[world.getN()][world.getN()];
        boolean[][] closed = new boolean[world.getN()][world.getN()];
        Node origin = getOriginNode(startPoint.toImmutable());
        open.add(origin);
        while (!open.isEmpty()) {
            Node node = open.poll();
            if (node == null) {
                throw new IllegalStateException();
            }
            Point point = node.getPoint();
            if (isDestination.test(point)) {
                T result = consumePath.apply(createPath(node));
                if (result != null) {
                    return result;
                }
            }
            Point pos = node.getPoint();
            // Check all neighbors of the current node
            for (Direction dir : Direction.DIRECTIONS) {
                Position neighborPos = cachedPos.set(pos, dir);
                if (!neighborPos.checkBounds(world.getN()) || closed[neighborPos.getY()][neighborPos.getX()]) {
                    continue;
                }
                if (validNeighbor != null && !validNeighbor.test(neighborPos)) {
                    continue;
                }
                // If the node is already in the queue and not closed update the cost value
                boolean alreadyInQueue = inQueue[neighborPos.getY()][neighborPos.getX()];
                Node neighborNode = getNode(neighborPos, node, alreadyInQueue);
                if (alreadyInQueue) {
                    // The queue only updates the entry position in the data structure if the object is remove and added
                    // again
                    open.remove(neighborNode);
                }
                open.add(neighborNode);
                inQueue[neighborPos.getY()][neighborPos.getX()] = true;
            }
            closed[point.getY()][point.getX()] = true;
        }
        return defaultValue.get();
    }

    /**
     * Tries to find the path to all materials from the factory as the origin point.
     *
     * @return A list of the path to all material nodes.
     */
    public List<Path> findPathsToMaterial() {
        return createPaths(factoryPos, p -> p.hasMaterials(world), world.getN() * 2, world.getN());
    }

    /**
     * Tries to find the path to as many destinations as the limit parameter allows.
     *
     * @param startPoint Origin of the path
     * @param isDestination Tests if the given position is a valid destination.
     * @param limit Limits the amount of paths, stops the search if this limit is reached.
     * @param distanceLimit Limits the distance the algorithm searches for possible path nodes.
     *
     * @return A list of all found paths with a size from zero, if no paths were found, up to the limit.
     */
    public List<Path> createPaths(Position startPoint, Predicate<Point> isDestination, int limit, int distanceLimit) {
        List<Path> paths = new ArrayList<>();
        findPath(startPoint, isDestination, neighborPos -> neighborPos.checkBounds(startPoint, distanceLimit), p -> {
            paths.add(p);
            return paths.size() >= limit ? paths : null;
        }, () -> paths);
        return paths;
    }

    public List<Path> createPaths(Position startPoint, Predicate<Point> isDestination, Collection<Point> destinations, int limit, int expansionDivisor) {
        int expansion = 5 + world.getN() / expansionDivisor;
        Bounds bounds = Bounds.create(world, expansion, destinations);
        List<Path> paths = new ArrayList<>();
        findPath(startPoint, isDestination, bounds::contains, p -> {
            paths.add(p);
            return paths.size() >= limit ? paths : null;
        }, () -> paths);
        return paths;
    }

    /**
     * Tries to find a path starting from the given origin position.
     *
     * @param startPoint Origin of the path
     * @param isDestination Tests if the given position is a valid destination.
     *
     * @return The found path, {@code null} if none was found.
     */
    public Path findPath(Position startPoint, Predicate<Point> isDestination) {
        lastGeneration = ++generation;
        return findPath(startPoint, isDestination, null, p -> p, () -> null);
    }

    /**
     * Creates a path starting at the given node and traversing the parent nodes until a node is found where the parent
     * is null. This position is the origin.
     */
    public Path createPath(Node node) {
        List<Path.Step> steps = new ArrayList<>();
        while (node != null) {
            // Create steps starting on the destination
            Path.Step step = node.toStep();
            node = node.getRoot();
            steps.add(step);
        }
        // Reverse steps, so they are in the right order and the first element is the origin
        Collections.reverse(steps);
        return new Path(steps.toArray(new Path.Step[0]));
    }

    /**
     * Helper class which represents a position on the field. Used to store the cost of this position, which does not
     * change in the lifetime of this program, and the accumulated cost of the current algorithm run.
     */
    private static class Node implements Comparable<Node> {
        /**
         * Position on the field
         */
        private final Point point;
        /**
         * Field cost
         */
        private final int timeCost;
        private final int m;
        /**
         * Current cost up to this position, starting from the origin
         */
        private int g;
        /**
         * Root object, used to create the path if a destination is found.
         */
        private Node root;

        public Node(Point point, int timeCost, int m) {
            this.point = point;
            this.timeCost = timeCost;
            this.m = m;
        }

        public void init(Node root) {
            this.root = root;
            this.g = (root != null ? root.g : 0) + timeCost;
        }

        public void tryUpdate(Node root) {
            if (root.g + timeCost >= g) {
                return;
            }
            this.g = root.g + timeCost;
            this.root = root;
        }

        public Node getRoot() {
            return root;
        }

        public int getCost() {
            return (g) * m;
        }

        public int getTimeCost() {
            return timeCost;
        }

        public Point getPoint() {
            return point;
        }

        @Override
        public int compareTo(Node o) {
            return getCost() - o.getCost();
        }

        /**
         * Converts this node to a path step.
         */
        public Path.Step toStep() {
            // Destination
            if (root == null) {
                return new Path.Step(point, null, getTimeCost());
            }
            return new Path.Step(point, root.getPoint().dirTo(point), getTimeCost());
        }
    }
}
