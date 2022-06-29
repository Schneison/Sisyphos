import com.google.common.base.Stopwatch;
import robot.Robot;
import robot.World;

import java.util.*;
import java.util.function.Predicate;

public interface RoutingStrategy {

    void drive(Environment env, World world);

    class BundleStrategy implements RoutingStrategy {
        public static final int NEIGHBOR_LIMIT = 7;//7=1000,3=50,5=100

        private final PathStore.Config config = new PathStore.Config(n-> 5 + n / 2,NEIGHBOR_LIMIT);
        private final PathStore.Config secondConfig = new PathStore.Config(n->n * 2, NEIGHBOR_LIMIT);
        private final PathStore.Config thirdConfig = new PathStore.Config(n->n * 2,Integer.MAX_VALUE);

        @Override
        public void drive(Environment env, World world) {
            env.setupStore(config);
            PathStore store = env.getStore();
            CompositorSeason season = CompositorSeason.fromStore(store);
            ClusterCompositor compositor = new ClusterCompositor(season);
            Set<Cluster> clusters = compositor.tryCreate(world);
            Bundle bundle = new Bundle(clusters);
            while (season.hasRemaining()) {
                season = CompositorSeason.fromSeason(season, store, secondConfig);
                compositor = new ClusterCompositor(season);
                clusters.addAll(compositor.tryCreate(world));
                bundle = new Bundle(clusters);
            }
            ClusterOptimiser optimiser = new ClusterOptimiser(clusters, env);

            System.out.println("Bundle Time: " + bundle.getTime());
            bundle = new Bundle(optimiser.process(thirdConfig));
            System.out.println("Bundle Time: " + bundle.getTime());
            bundle.drive(env);
        }
    }

    class StraightStrategy implements RoutingStrategy {
        @Override
        public void drive(Environment env, World world) {
            Robot robot = env.getRobot();
            PathCreator creator = env.getCreator();
            env.setupStore(new PathStore.Config(n->5 + n / 2, 0));
            PathStore store = env.getStore();
            Predicate<Point> materialDestination = p -> p.hasMaterials(world);
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
                        Log.debug("From:" + nextPath.getOriginPos() + ",To:" + nextPath.getDestinationPos() + ",T:" + nextPath.getTimeCost() + ",S:" + nextPath.getStepCount());
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
                    Log.debug("Home: From:" + factoryPath.getOriginPos() + ",To:" + factoryPath.getDestinationPos() + ",T:" + factoryPath.getTimeCost() + ",S:" + factoryPath.getStepCount());
                    factoryPath.drive(robot, analytics);
                    robot.unloadMaterials();
                    totalMaterials -= materials;
                    materials = 0;
                }
                Log.debug("Materials Remaining: " + totalMaterials);
            }
        }
    }
}
