import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;
import robot.Factory;
import robot.Robot;
import robot.World;

class RobotController {

    public static int processingDuration;
    private static final String LOG_PATH = "C:/Users/Larson/OneDrive/Documents/Studium/2. Semester/Algorithmen und Datenstrukturen/Zusatz/logs/";

    public static void main(String[] args) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        if (args.length != 1) {
            System.out.println("Usage: java RobotController n");
            return;
        }
        int n = Integer.parseInt(args[0]);
        World world = new World(n);
        processingDuration = world.getN() / 2;
        Robot robot = world.getRobot();
        Factory factory = world.getFactory();
        /*FileHandler fh;
        SimpleDateFormat format = new SimpleDateFormat("M-d_HHmmss");
        fh = new FileHandler(LOG_PATH + "factor_test_" + format.format(Calendar.getInstance().getTime()) + ".log");
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        //        fh.setFormatter(formatter);
        fh.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                SimpleDateFormat logTime = new SimpleDateFormat("HH:mm:ss");
                Calendar cal = new GregorianCalendar();
                cal.setTimeInMillis(record.getMillis());
                return "[" + logTime.format(cal.getTime()) + "],["
                        + record.getLevel()
                        + "]: "
                        + record.getMessage() + "\n";
            }
        });*/
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


