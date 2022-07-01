import robot.Robot;
import robot.World;

import java.util.*;
import java.util.stream.Stream;

public class Cluster implements Comparable<Cluster> {
    public static final int DISTANCE_FACTOR = Environment.CLUSTER_FACTOR;
    //0 - fromFactory
    // between - materials
    //length - 1 - toFactory
    public final Path[] paths;
    public final int totalTime;
    private final Combination combination;
    private final int distance;

    public Cluster(Path fromFactory, Path toFactory, Path... materials) {
        this.paths = new Path[materials.length + 2];
        this.paths[0] = fromFactory;
        this.paths[materials.length + 1] = toFactory;
        int time = fromFactory.getTimeCost() + toFactory.getTimeCost();
        for (int i = 1; i < materials.length + 1; i++) {
            paths[i] = materials[i - 1];
            time += materials[i - 1].getTimeCost();
        }
        this.totalTime = time;
        this.combination = new Combination(
                fromFactory.getDestinationPos(),
                materials.length > 1 ? materials[0].getDestinationPos() : null,
                toFactory.getOriginPos()
        );
        this.distance = combination.generateDistance();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cluster that = (Cluster) o;
        return combination.equals(that.combination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(combination);
    }

    public Type getType(){
        return combination.getType();
    }

    /**
     * Checks if the cluster has three unique positions.
     */
    public boolean isNormal(){
        return getType() == Type.NORMAL;
    }

    public Path[] getPaths() {
        return paths;
    }

    public Point[] getPoints() {
        return combination.asArray();
    }

    public static int sumChunkTime(Collection<Cluster> clusters) {
        return sumChunkTime(clusters.stream());
    }

    public static int sumChunkTime(Stream<Cluster> clusters) {
        return clusters
                .mapToInt(Cluster::getTotalTime)
                .sum();
    }

    public int getTotalTime() {
        return totalTime;
    }

    public int getCost() {
        return Math.abs(getTotalTime() + distance * DISTANCE_FACTOR);
    }

    @Override
    public int compareTo(Cluster o) {
        return getCost() - o.getCost();
    }

    public void removeUsed(CompositorSeason season) {
        combination.removeUsed(season);
    }

    public long penaltyTotal;

    public void drive(Environment env) {
        World world = env.getWorld();
        Robot robot = env.getRobot();
        int materials = 0;
        for (Path path : paths) {
            path.drive(robot, env.getAnalytics());
            if (path.getMaterial(world) > 0) {
                materials += path.getMaterial(world);
                robot.gatherMaterials();
            }
        }
        robot.unloadMaterials();
    }

    public enum Type {
        NORMAL, SOLO, DOUBLE
    }

    public static class Combination {
        public final Point origin;
        public final Point material;
        public final Point destination;

        public Combination(Point origin, Point material, Point destination) {
            this.origin = origin;
            this.material = material;
            this.destination = destination;
        }

        public int generateDistance() {
            int aX = MathUtil.diffX(origin, destination);
            int aY = MathUtil.diffY(origin, destination);
            int w = 0;
            int h = 0;
            if (material != null) {
                int bX = MathUtil.diffX(material, destination);
                int bY = MathUtil.diffY(material, destination);
                int cX = MathUtil.diffX(material, origin);
                int cY = MathUtil.diffY(material, origin);
                w = Math.max(bX, cX);
                h = Math.max(bY, cY);
            }
            w = Math.max(aX, w);
            h = Math.max(aY, h);
            return w + h;
        }

        public void removeUsed(CompositorSeason store) {
            store.usePositions(origin, destination, material);
        }

        @Override
        public String toString() {
            return "origin=" + origin +
                    ", material=" + material +
                    ", destination=" + destination;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Combination that = (Combination) o;
            return (origin.equals(that.origin) || origin.equals(that.destination))
                    && Objects.equals(material, that.material)
                    && (destination.equals(that.destination) || destination.equals(that.origin));
        }

        @Override
        public int hashCode() {
            return (origin.hashCode() + destination.hashCode()) * 37 + (material != null ? material.hashCode() : 0);
        }

        public Point[] asArray() {
            Type type = getType();
            return switch (type){
                case SOLO -> new Point[]{origin};
                case DOUBLE -> new Point[]{origin, destination};
                case NORMAL -> new Point[]{origin, material, destination};
            };
        }

        public Type getType() {
            Type type = Type.NORMAL;
            if(material == null){
                type = origin.equals(destination) ? Type.SOLO : Type.DOUBLE;
            }
            return type;
        }
    }
}
