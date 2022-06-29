import java.util.HashMap;
import java.util.Map;

public class TimeLookup {
    private final int[] factory;
    private final int[][] edges;
    private int posIndex;
    private final Map<Point, Integer> pointToIndex;

    public TimeLookup(int n) {
        this.factory = new int[n];
        this.edges = new int[n][n];
        this.pointToIndex = new HashMap<>();
    }

    public void setEdge(Point from, Point to, int time) {
        int a = pointToIndex.computeIfAbsent(from, p -> posIndex++);
        int b = pointToIndex.computeIfAbsent(to, p -> posIndex++);
        edges[a][b] = time;
    }


    public void setFactory(Point pos, int time) {
        int i = pointToIndex.computeIfAbsent(pos, p -> posIndex++);
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
