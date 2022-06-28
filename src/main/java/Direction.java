public enum Direction {
    LEFT, RIGHT, DOWN, UP;

    public static final Direction[] DIRECTIONS = values();

    public Direction invert() {
        return switch (this) {
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case UP -> DOWN;
            case RIGHT -> LEFT;
        };
    }
}
