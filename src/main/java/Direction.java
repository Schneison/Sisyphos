/**
 * Defines all four possible directions on the field.
 */
public enum Direction {
    LEFT, RIGHT, DOWN, UP;

    static final Direction[] DIRECTIONS = values();

    public Direction opposite() {
        return switch (this) {
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case UP -> DOWN;
            case RIGHT -> LEFT;
        };
    }
}
