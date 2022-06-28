import robot.World;

/**
 * Abstract implementation of a 2d position on the field.
 */
public abstract class Position {

    /**
     * X-Coordinate
     */
    public abstract int getX();

    /**
     * y-Coordinate
     */
    public abstract int getY();

    /**
     * Converts this position to an immutable version, returns the same instance if the position is already immutable.
     */
    public abstract Point toImmutable();

    /**
     * Offsets the positions by the given values on the x and y axis.
     */
    public abstract Position offset(int xOffset, int yOffset);

    /**
     * Checks if the has the same coordinates as the given values.
     */
    public boolean at(int x, int y) {
        return this.getX() == x && y == this.getY();
    }

    /**
     * Checks if the has the same coordinates as the given values.
     */
    public boolean at(Position pos) {
        return at(pos.getX(), pos.getY());
    }

    /**
     * Retrieves the amount of material currently at that position.
     */
    public int getMaterials(World world) {
        return world.getFieldMaterials(getX(), getY());
    }

    /**
     * Checks if there are any materials at this position.
     */
    public boolean hasMaterials(World world) {
        return getMaterials(world) > 0;
    }

    /**
     * Retrieves the time that the roboter would need to move onto this position on the field.
     */
    public int getTime(World world) {
        return world.getFieldTime(getX(), getY());
    }

    /**
     * Offsets the position by the given direction.
     */
    public Position offset(Direction dir) {
        return switch (dir) {
            case DOWN -> offset(0, -1);
            case UP -> offset(0, 1);
            case LEFT -> offset(-1, 0);
            case RIGHT -> offset(1, 0);
        };
    }

    /**
     * Calculates the direction that the given position is oriented in relation to this pos.
     */
    public Direction dirTo(Point other) {
        int xDiff = other.getX() - getX();
        int yDiff = other.getY() - getY();
        if (xDiff > 0) {
            return Direction.RIGHT;
        }
        if (xDiff < 0) {
            return Direction.LEFT;
        }
        if (yDiff > 0) {
            return Direction.DOWN;
        }
        if (yDiff < 0) {
            return Direction.UP;
        }
        throw new IllegalStateException();
    }

    /**
     * Checks if the position is contained an area of the given size.
     */
    public boolean checkBounds(int size) {
        return getX() >= 0 && getX() < size && getY() >= 0 && getY() < size;
    }

    /**
     * Checks if the position is contained an area of the given size and with the given position as the center.
     */
    public boolean checkBounds(Position point, int size) {
        return getX() > point.getX() - size && getX() < point.getX() + size && getY() > point.getY() - size && getY() < point.getY() + size;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position pos)) {
            return false;
        }
        return getX() == pos.getX() && getY() == pos.getY();
    }

    @Override
    public int hashCode() {
        return 37 * getX() + getY();
    }

    @Override
    public String toString() {
        return "x=" + getX() + ", y=" + getY();
    }
}
