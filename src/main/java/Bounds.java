import robot.World;

import java.util.Collection;

/**
 * Helper class to construct a rectangle.
 */
public class Bounds {
    private final int startX;
    private final int startY;
    private final int endX;
    private final int endY;

    /**
     * Construct a rectangle that contains all given positions.
     * <p>
     *
     * @param world     The world instance to determine the size of the field.
     * @param expansion Amount by that the rectangle is expanded.
     * @param positions A collection of positions that define the rectangle
     */
    public static Bounds create(World world, int expansion, Collection<Point> positions) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Point p : positions) {
            minX = Math.min(p.getX(), minX);
            minY = Math.min(p.getY(), minY);
            maxX = Math.max(p.getX(), maxX);
            maxY = Math.max(p.getY(), maxY);
        }
        int areaStartX = Math.max(minX - expansion, 0);
        int areaStartY = Math.max(minY - expansion, 0);
        int areaEndX = Math.min(maxX + expansion, world.getN());
        int areaEndY = Math.min(maxY + expansion, world.getN());
        return new Bounds(areaStartX, areaStartY, areaEndX, areaEndY);
    }

    private Bounds(int startX, int startY, int endX, int endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    /**
     * Checks if the given position is located in the rectangle.
     */
    public boolean contains(Position p) {
        return p.getX() >= startX &&
                p.getX() <= endX &&
                p.getY() >= startY &&
                p.getY() <= endY;
    }
}
