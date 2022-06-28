import java.util.Set;

public class Chunk {
    public int time;
    public final Set<Cluster> clusters;

    public Chunk(Set<Cluster> clusters) {
        this.clusters = clusters;
        this.time = Cluster.sumChunkTime(clusters);
    }
}
