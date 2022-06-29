/**
 * Implementation of a immutable 2d position.
 */
public class Point extends Position {
    private final int x;
    private final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public Point offset(int x, int y) {
        return new Point(this.x + x, this.y + y);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public Point toImmutable() {
        return this;
    }
}
