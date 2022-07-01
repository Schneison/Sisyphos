import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;
import robot.Factory;
import robot.Robot;
import robot.World;

class RobotController {

    public static void main(String[] args) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        if (args.length != 1) {
            System.out.println("Usage: java RobotController n");
            return;
        }
        int n = Integer.parseInt(args[0]);
        World world = new World(n);
        Robot robot = world.getRobot();
        Factory factory = world.getFactory();
        if (!stopwatch.isRunning()) {
            stopwatch.reset();
            stopwatch.start();
        }
        //Create field environment
        Environment env = new Environment(world, robot, factory);
        // Select routing strategy, mainly used for testing
        RoutingStrategy strategy = new RoutingStrategy.BundleStrategy();

        strategy.drive(env, world);
        //fh.close();
        factory.waitTillFinished();
        System.out.println("Remaining materials: " + world.getTotalMaterials()); // should be 0
        System.out.println("Time passed: " + world.getTimePassed());

        stopwatch.stop();
        System.out.println("Time: " + stopwatch.elapsed().toMinutesPart() + ":" + stopwatch.elapsed().toSecondsPart());
    }


}


