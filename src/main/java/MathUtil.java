import java.util.Arrays;
import java.util.Random;

/**
 * Utility class for all math or array related methods.
 */
public class MathUtil {

    private static Random r;

    private MathUtil() {
        // Don't allow instances of utility classes
    }

    /**
     * Calculates the absolute difference on the x-axis of the two given positions.
     */
    public static int diffX(Position a, Position b) {
        return Math.abs(a.getX() - b.getX());
    }

    /**
     * Calculates the absolute difference on the y-axis of the two given positions.
     */
    public static int diffY(Position a, Position b) {
        return Math.abs(a.getY() - b.getY());
    }

    /**
     * Swaps the value on the first index with the value on the second index of the given array.
     */
    public static void swap(int[] data, int a, int b) {
        if (a == b) {
            return;
        }
        int temp = data[a];
        data[a] = data[b];
        data[b] = temp;
    }

    /**
     * Shuffles the given array.
     */
    public static int[] shuffle(int[] data) {
        if (r == null) {
            r = new Random(110);
        }
        for (int i = data.length; i > 1; i--)
            MathUtil.swap(data, i - 1, r.nextInt(i));
        return Arrays.copyOf(data, data.length);
    }
}
