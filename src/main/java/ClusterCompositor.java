import robot.World;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class ClusterCompositor {

    private final CompositorSeason season;

    public ClusterCompositor(CompositorSeason season) {
        this.season = season;
    }

    public Set<Cluster> tryCreate(World world) {
        PriorityQueue<Path> bestPaths = season.getBestPaths();
        Set<Cluster> clusters = new HashSet<>();
        //Build single node cluster it is needed, but try to avoid this
        if (bestPaths.size() == 1) {
            Path deliveryPath = bestPaths.peek();
            Cluster cluster = new Cluster(deliveryPath.invert(), deliveryPath);
            cluster.removeUsed(season);
            clusters.add(cluster);
            return clusters;
        }
        // Discover other materials
        for (Path path : bestPaths) {
            PriorityQueue<Cluster> test = addClusters(new PriorityQueue<>(), path.invert(), world);
            Cluster bestPackage = test.poll();
            if (bestPackage != null) {
                bestPackage.removeUsed(season);
                clusters.add(bestPackage);
            }
        }
        return clusters;
    }

    public <T extends Collection<Cluster>> T addClusters(T packages, Path factoryPath, World world) {
        int originMat = factoryPath.getMaterial(world);
        Point firstMaterial = factoryPath.getDestinationPos();
        List<Path> paths = season.getNeighbors(firstMaterial);
        if (paths == null) {
            return packages;
        }
        for (Path firstPath : paths) {
            int secondMat = originMat + firstPath.getMaterial(world);
            if (secondMat == 3) {
                Path deliveryPath = season.toFactory(firstPath.getDestinationPos());
                if (deliveryPath == null) {
                    continue;
                }
                packages.add(new Cluster(factoryPath, deliveryPath, firstPath));
                continue;
            }
            if (secondMat < 3 && season.getRemainingCount() > 2) {
                List<Path> secondPaths = season.getNeighbors(firstPath.getDestinationPos());
                // Only if path was already used by other packages
                if (secondPaths == null) {
                    continue;
                }
                for (Path secondPath : secondPaths) {
                    if (secondPath.getDestinationPos().equals(firstMaterial) || secondPath.getDestinationPos().equals(firstPath.getDestinationPos())) {
                        continue;
                    }
                    int lastMat = secondMat + secondPath.getMaterial(world);
                    // Always try to only visit nodes one time, so we have to check if the nodes are empty after visit
                    // which only is the case if the sum is less or equal to 3
                    if (lastMat > 3) {
                        continue;
                    }
                    Path deliveryPath = season.toFactory(secondPath.getDestinationPos());
                    if (deliveryPath == null) {
                        continue;
                    }
                    packages.add(new Cluster(factoryPath, deliveryPath, firstPath, secondPath));
                }
            } else {
                Path deliveryPath = season.toFactory(firstPath.getDestinationPos());
                if (deliveryPath == null) {
                    continue;
                }
                packages.add(new Cluster(factoryPath, deliveryPath, firstPath));
            }
        }
        return packages;
    }
}
