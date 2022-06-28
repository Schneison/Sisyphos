import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TimeLookup {
    private final int[] factory;
    private final int[][] edges;
    private int index;
    private final Map<Point, Integer> pointToIndex;

    public TimeLookup(int n) {
        this.factory = new int[n];
        this.edges = new int[n][n];
        this.pointToIndex = new HashMap<>();
    }

    public void setEdge(Point from, Point to, int time) {
        int a = pointToIndex.computeIfAbsent(from, (p) -> index++);
        int b = pointToIndex.computeIfAbsent(to, (p) -> index++);
        int tA = edges[a][b];
        int tB = edges[b][a];
        if (tA != tB) {
            System.out.println();
        }
        edges[a][b] = time;
        edges[b][a] = time;
    }


    public void setFactory(Point pos, int time) {
        int i = pointToIndex.computeIfAbsent(pos, (p) -> index++);
        factory[i] = time;
    }

    public int getEdge(Point from, Point to) {
        Integer a = pointToIndex.get(from);
        Integer b = pointToIndex.get(to);
        if (a == null || b == null) {
            throw new IllegalStateException();
        }
        if (edges[a][b] == 0) {
            System.out.println("Failed to find connection from {" + from + "} to {" + to + "}.");
        }
        return edges[a][b];
    }

    public int toFactory(Point pos) {
        Integer index = pointToIndex.get(pos);
        if (index == null) {
            throw new IllegalStateException();
        }
        return factory[index];
    }
}
