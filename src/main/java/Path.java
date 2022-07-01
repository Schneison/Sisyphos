import robot.Robot;
import robot.World;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Path implements Comparable<Path> {
    private final Step[] steps;
    private final Step destination;
    private final Step origin;
    private int current;
    private int time = -1;

    public Path(Step[] steps) {
        this.steps = steps;
        this.destination = steps[steps.length - 1];
        this.origin = steps[0];
    }

    @Override
    public int compareTo(Path o) {
        return getTimeCost() - o.getTimeCost();
    }

    public void drive(Robot robot, Analytics analytics) {
        while (!atDestination(robot)) {
            move(robot, analytics);
        }
    }

    public Path invert() {
        List<Step> reverseSteps = new LinkedList<>();
        for (Step step : steps) {
            reverseSteps.add(step.invert());
        }
        Collections.reverse(reverseSteps);
        return new Path(reverseSteps.toArray(new Step[0]));
    }

    public void move(Robot robot, Analytics analytics) {
        // At destination
        if (current == steps.length) {
            return;
        }
        Step step = steps[current];
        analytics.getCellOrCreate(step.getPoint()).onEnter(step.getPoint(), current == 0 || current == (steps.length - 1));
        current++;
        if (step.getDirection() == null) {
            return;
        }
        switch (step.getDirection()) {
            case DOWN -> robot.moveDown();
            case LEFT -> robot.moveLeft();
            case RIGHT -> robot.moveRight();
            case UP -> robot.moveUp();
        }
    }

    public int getMaterial(World world) {
        return destination.getMaterial(world);
    }

    public int getTimeCost() {
        if (time >= 0) {
            return time;
        }
        time = 0;
        for (Step point : steps) {
            if (point == origin) {
                continue;
            }
            time += point.getTime();
        }
        return time;
    }

    public Point getDestinationPos() {
        return destination.getPoint();
    }

    public Point getOriginPos() {
        return origin.getPoint();
    }

    public boolean atDestination(Robot robot) {
        return destination.contains(robot);
    }

    /**
     * Amount of steps in this path.
     */
    public int getStepCount() {
        return steps.length;
    }

    @Override
    public String toString() {
        return "orig={"+ origin.getPoint() + "},dest={" + destination.getPoint() + "},steps=" + getStepCount();
    }

    public static class Step {
        private final Point point;
        // TODO: Remove possible value null, wtf
        private final Direction direction;
        private final int time;

        public Step(Point point, Direction direction, int time) {
            this.point = point;
            this.direction = direction;
            this.time = time;
        }

        public Direction getDirection() {
            return direction;
        }

        public Step invert() {
            return new Step(point, direction != null ? direction.opposite() : null, time);
        }

        public int getMaterial(World world) {
            return point.getMaterials(world);
        }

        public Point getPoint() {
            return point;
        }

        public int getTime() {
            return time;
        }

        public boolean contains(Robot robot) {
            return robot.getX() == point.getX() && robot.getY() == point
                    .getY();
        }

    }
}
