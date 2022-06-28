import robot.Robot;
import robot.World;

import java.util.Set;

public class Bundle implements Comparable<Bundle> {
    private final Set<Cluster> clusters;
    private final int time;
    private final int penalty;

    public Bundle(Set<Cluster> clusters, World world) {
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

    public int getTime() {
        return time;
    }

    @Override
    public int compareTo(Bundle o) {
        return o.getTime() - getTime();
    }

    public void drive(Environment env) {
        for (Cluster cluster : clusters) {
            cluster.drive(env);
        }
    }

    public Set<Cluster> getClusters() {
        return clusters;
    }
}
