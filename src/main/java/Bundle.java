import java.util.Set;

/**
 * Collection of clusters.
 */
public class Bundle implements Comparable<Bundle> {
    private final Set<Cluster> clusters;
    /**
     * Accumulated time cost
     */
    private final int time;
    /**
     * Amount of time penalty, created by robot if the time cost of a cluster
     * is smaller that half the size of the field.
     */
    private final int penalty;

    public Bundle(Set<Cluster> clusters) {
        this.clusters = clusters;
        int timeSum = 0;
        int timePenalty = RobotController.processingDuration; // Last delivered package always cost 1 penalty
        for (Cluster cluster : clusters) {
            timeSum += cluster.getTotalTime();
            if (cluster.getTotalTime() < RobotController.processingDuration) {
                timePenalty += (RobotController.processingDuration - cluster.getTotalTime());
            }
        }
        this.penalty = timePenalty;
        this.time = timeSum + timePenalty;
    }

    /**
     * The accumulated time cost of all contained clusters.
     */
    public int getTime() {
        return time;
    }

    @Override
    public int compareTo(Bundle o) {
        return o.getTime() - getTime();
    }

    /**
     * Drives the robot to the clusters after each other
     */
    public void drive(Environment env) {
        for (Cluster cluster : clusters) {
            cluster.drive(env);
        }
    }
}
