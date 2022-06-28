import com.google.common.base.Stopwatch;
import robot.Robot;
import robot.World;

import java.util.*;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

public abstract class RoutingStrategy {

    abstract void drive(Environment env, World world);

    public static class BundleStrategy extends RoutingStrategy {
        public static final int NEIGHBOR_LIMIT = 7;//7=1000,3=50,5=100

        private final PathStore.Config config = new PathStore.Config((n)-> 5 + n / 2,NEIGHBOR_LIMIT);
        private final PathStore.Config secondConfig = new PathStore.Config((n)->n * 2, NEIGHBOR_LIMIT);
        private final PathStore.Config thirdConfig = new PathStore.Config((n)->n * 2,Integer.MAX_VALUE);

        @Override
        void drive(Environment env, World world) {
            env.setupStore(config);
            PathCreator creator = env.getCreator();
            Cluster.factor = 31;//31=1000,15=50,10=100
            PathStore store = env.getStore();
            CompositorSeason season = CompositorSeason.fromStore(store);
            ClusterCompositor compositor = new ClusterCompositor(season);
            Set<Cluster> clusters = compositor.tryCreate(world);
            Bundle bundle = new Bundle(clusters, world);
            while (season.hasRemaining()) {
                season = CompositorSeason.fromSeason(season, store, creator, secondConfig);
                compositor = new ClusterCompositor(season);
                clusters.addAll(compositor.tryCreate(world));
                bundle = new Bundle(clusters, world);
            }
            ClusterOptimiser optimiser = new ClusterOptimiser(clusters, store);

            System.out.println("Bundle Time: " + bundle.getTime());
            bundle = new Bundle(optimiser.process(creator, thirdConfig), world);
            System.out.println("Bundle Time: " + bundle.getTime());
            bundle.drive(env);
        }
    }

    public static class StraightStrategy extends RoutingStrategy {
        @Override
        void drive(Environment env, World world) {
            Robot robot = env.getRobot();
            PathCreator creator = env.getCreator();
            env.setupStore(new PathStore.Config((n)->5 + n / 2, 0));
            PathStore store = env.getStore();
            Predicate<Point> materialDestination = (p) -> p.hasMaterials(world);
            Analytics analytics = env.getAnalytics();
            Stopwatch stopwatch = Stopwatch.createUnstarted();
            int totalMaterials = world.getTotalMaterials();
            int materials = 0;
            while (totalMaterials > 0) {
                int maxMaterials = Math.min(3, totalMaterials);
                while (materials < maxMaterials) {
                    stopwatch.start();
                    Path nextPath = creator.findPath(new Point(robot.getX(), robot.getY()), materialDestination);
                    stopwatch.stop();
                    stopwatch.reset();

                    if (nextPath != null) {
                        analytics.addPath(nextPath);
                        stopwatch.start();
                        nextPath.drive(robot, analytics);
                        //System.out.println("From:" + nextPath.getOriginPos() + ",To:" + nextPath.getDestinationPos() + ",T:" + nextPath.getTimeCost() + ",S:" + nextPath.getStepCount());
                        materials = Math.min(materials + nextPath.getMaterial(world), maxMaterials);
                        robot.gatherMaterials();
                        stopwatch.stop();
                        stopwatch.reset();
                    }
                }
                stopwatch.start();
                Path factoryPath = store.getPathToFactory(new Point(robot.getX(), robot.getY()));
                stopwatch.stop();
                stopwatch.reset();
                if (factoryPath != null) {
                    analytics.addPath(factoryPath);
                    //System.out.println("Home: From:" + factoryPath.getOriginPos() + ",To:" + factoryPath.getDestinationPos() + ",T:" + factoryPath.getTimeCost() + ",S:" + factoryPath.getStepCount());
                    factoryPath.drive(robot, analytics);
                    robot.unloadMaterials();
                    totalMaterials -= materials;
                    materials = 0;
                }
                //System.out.println("Materials Remaining: " + totalMaterials);
            }
        }
    }
}
