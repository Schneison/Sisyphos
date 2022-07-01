import robot.Factory;
import robot.Robot;
import robot.World;

public class Environment {
    public static final boolean LOGGING = false;
    public static final boolean DEBUG = false;
    public static final int CLUSTER_FACTOR = 31;
    public static int processingDuration;

    private final World world;
    private final Robot robot;
    private final Factory factory;
    private final PathCreator creator;
    private PathStore store;

    public Environment(World world, Robot robot, Factory factory) {
        this.world = world;
        this.robot = robot;
        this.factory = factory;
        this.creator = new PathCreator(this, new Point(factory.getX(), factory.getY()));
        processingDuration = world.getN() / 2;
    }

    public void setupStore(PathStore.Config config) {
        store = new PathStore(this, config);
    }

    public PathCreator getCreator() {
        return creator;
    }

    public PathStore getStore() {
        return store;
    }

    public TimeLookup getLookup() {
        return store.getLookup();
    }

    public World getWorld() {
        return world;
    }

    public Robot getRobot() {
        return robot;
    }

    public Factory getFactory() {
        return factory;
    }
}
