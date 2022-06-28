import java.util.Arrays;
import java.util.Random;

public class MathUtil {

    private static Random r;

    public static int diffX(Position a, Position b) {
        return Math.abs(a.getX() - b.getX());
    }

    public static int diffY(Position a, Position b) {
        return Math.abs(a.getY() - b.getY());
    }

    public static void swap(int[] data, int a, int b) {
        if (a == b) {
            return;
        }
        int temp = data[a];
        data[a] = data[b];
        data[b] = temp;
    }

    public static int[] shuffle(int[] data) {
        if (r == null) {
            r = new Random(110);
        }
        for (int i = data.length; i > 1; i--)
            MathUtil.swap(data, i - 1, r.nextInt(i));
        return Arrays.copyOf(data, data.length);
    }
}
