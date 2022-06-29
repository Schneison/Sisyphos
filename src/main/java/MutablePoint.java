import java.util.Objects;

/**
 * Implementation of a mutable 2d position.
 * <p>
 * This is used to reduce the allocations of new position objects that are only temporary.
 */
public class MutablePoint extends Position {
    private int x;
    private int y;

    public MutablePoint() {
        this(0, 0);
    }

    public MutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getX(), getY());
    }

    public void set(Position pos) {
        this.x = pos.getX();
        this.y = pos.getY();
    }

    public Position set(Position pos, Direction dir) {
        set(pos);
        return switch (dir) {
            case DOWN -> offset(0, -1);
            case UP -> offset(0, 1);
            case LEFT -> offset(-1, 0);
            case RIGHT -> offset(1, 0);
        };
    }

    public MutablePoint offset(int x, int y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public Point toImmutable() {
        return new Point(x, y);
    }
}
