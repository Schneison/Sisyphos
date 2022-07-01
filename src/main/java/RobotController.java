import robot.Factory;
import robot.Robot;
import robot.World;

class RobotController {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java RobotController n");
            return;
        }
        int n = Integer.parseInt(args[0]);
        World world = new World(n);
        Robot robot = world.getRobot();
        Factory factory = world.getFactory();

        //Create field environment
        Environment env = new Environment(world, robot, factory);

        RoutingStrategy.drive(env, world);

        factory.waitTillFinished();
        System.out.println("Remaining materials: " + world.getTotalMaterials()); // should be 0
        System.out.println("Time passed: " + world.getTimePassed());
    }


}


