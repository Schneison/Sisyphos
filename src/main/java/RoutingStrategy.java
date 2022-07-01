import robot.World;

import java.util.Set;

public class RoutingStrategy {
    public static final int NEIGHBOR_LIMIT = 7;
    private static final PathStore.Config config = new PathStore.Config(n -> 5 + n / 2, NEIGHBOR_LIMIT);
    private static final PathStore.Config secondConfig = new PathStore.Config(n -> n * 2, NEIGHBOR_LIMIT);
    private static final PathStore.Config thirdConfig = new PathStore.Config(n -> n * 2, Integer.MAX_VALUE);

    private RoutingStrategy() {
    }

    public static void drive(Environment env, World world) {
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

        bundle.print();
        bundle = new Bundle(optimiser.process(thirdConfig));
        bundle.print();
        bundle.drive(env);
    }
}
