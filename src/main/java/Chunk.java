import java.util.Set;

public class Chunk {
    private final int time;
    private final Set<Cluster> clusters;

    public Chunk(Set<Cluster> clusters) {
        this.clusters = clusters;
        this.time = Cluster.sumChunkTime(clusters);
    }

    public int getTime() {
        return time;
    }

    public Set<Cluster> getClusters() {
        return clusters;
    }
}
