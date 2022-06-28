import com.google.common.base.Stopwatch;

import java.util.*;

public class ClusterOptimiser {
    private final Queue queue;
    private final Set<State> dumps = new HashSet<>();
    private final Map<Point, Integer> clusterByPos;
    private final ClusterContainer[] clusters;
    private final PathStore store;
    private final State state;

    public ClusterOptimiser(Set<Cluster> clusters, PathStore store) {
        this.clusterByPos = new HashMap<>();
        this.state = new State();
        this.store = store;
        final int[] id = {0};
        this.clusters = new ClusterContainer[clusters.size()];
        clusters.stream().sorted(Cluster::compareTo).forEach((cluster)->{
            this.clusters[id[0]] = new ClusterContainer(cluster, id[0]);
            for (Point p : cluster.getPoints()) {
                clusterByPos.put(p, id[0]);
            }
            id[0] += 1;
        });
        this.queue = new Queue(this);
        initNeighbors();
    }

    private ClusterContainer getContainer(int id) {
        if (id >= clusters.length) {
            throw new IllegalStateException();
        }
        return clusters[id];
    }

    private ClusterContainer getContainer(Point pos) {
        Integer id = clusterByPos.get(pos);
        if (id == null) {
            throw new IllegalStateException();
        }
        return clusters[id];
    }

    private void tryAddNeighbor(Point pos, Point neighborPos) {
        ClusterContainer container = getContainer(pos);
        if (container == null) {
            throw new IllegalStateException();
        }
        ClusterContainer neighbor = getContainer(neighborPos);
        if (neighbor == null) {
            throw new IllegalStateException();
        }
        if (neighbor == container) {
            return;
        }
        container.addNeighbor(neighbor, neighborPos);
    }

    private void initNeighbors() {
        for (Map.Entry<Point, List<Path>> materialNode : store.getAllNeighbors().entrySet()) {
            Point pos = materialNode.getKey();
            for (Path neighborPath : materialNode.getValue()) {
                Point neighborPos = neighborPath.getDestinationPos();
                tryAddNeighbor(pos, neighborPos);
            }
        }
    }


    private void updateNeighbors(Set<Point> positions) {
        for (Point pos : positions) {
            for (Path neighborPath : store.getNeighborPaths(pos)) {
                Point neighborPos = neighborPath.getDestinationPos();
                tryAddNeighbor(pos, neighborPos);
            }
        }
    }

    public Set<Cluster> process(PathCreator creator,
                                PathStore.Config config) {
        Random rand = new Random(42);
        Stopwatch pathWatch = Stopwatch.createUnstarted();
        Stopwatch clusterWatch = Stopwatch.createUnstarted();
        System.out.println("Start Optimiser");
        Map<Integer, Integer> count = new HashMap<>();
        Map<Integer, Integer> count2 = new HashMap<>();
        Map<Integer, Integer> count3 = new HashMap<>();
        int factor = 0;
        o: for (int j = 0; j < 2200; j++) {
            ClusterContainer random = this.clusters[rand.nextInt(this.clusters.length)];
            if(random == null || j == 1198 || j == 1998){
                queue.reset(this.clusters);
                factor+=1;
               continue;
            }
            Cluster cluster = random.getCluster();
            Set<ClusterContainer> containers = new HashSet<>();
            Set<Cluster> clusters = new HashSet<>();
            Set<Point> positions = new HashSet<>(Arrays.asList(cluster.getPoints()));
            count3.compute(random.getId(), (i, c)->c == null ? 1 : c + 1);
            if (positions.size() % 3 != 0) {
                continue;
            }
            count2.compute(random.getId(), (i, c)->c == null ? 1 : c + 1);
            clusters.add(cluster);
            containers.add(random);
            List<Integer> neighbors = new ArrayList<>(random.getNeighbors());
            Collections.shuffle(neighbors, rand);
            for (int id : neighbors) {
                ClusterContainer container = getContainer(id);
                cluster = container.getCluster();
                if(!positions.addAll(Arrays.asList(cluster.getPoints()))){
                    // Cluster compliantly contained in other cluster, possibly 2 valued Material
                    continue;
                }
                clusters.add(cluster);
                containers.add(container);
                //We don't look at 2 valued Materials or single routes
                if (positions.size() % 3 != 0 || cluster.getPoints().length % 3 != 0) {
                    continue o;
                }
                count.compute(container.getId(), (i, c)->c == null ? 1 : c + 1);
                if (positions.size() >= (12 + 3 * factor)) {
                    break;
                }
            }
            int originalTime = Cluster.sumChunkTime(clusters);
            Chunk bestChunk = null;
            int minTime = Integer.MAX_VALUE;
            pathWatch.start();
            OptimiserVariant variant = new OptimiserVariant(
                    CompositorSeason.fromRange(store, positions, creator, config),
                    clusters,
                    store
            );
            pathWatch.stop();
            state.recordPath(pathWatch.toString());
            clusterWatch.start();
            for (int i = 0; i < 14; i++) {
                GeneticCluster geneticCluster = new GeneticCluster(150 + factor * 50, 0.025f, variant);
                Genome g = geneticCluster.run(120 + factor * 60, rand);
                Chunk result = new Chunk(variant.createClusters(g));
                if (result.time < minTime) {
                    minTime = result.time;
                    bestChunk = result;//-16249 - 6:10
                }
            }
            if (bestChunk == null) {
                throw new IllegalStateException();
            }
            clusterWatch.stop();
            state.recordCluster(clusterWatch.toString());

            //Add to current state and update queue if we changed the clusters
            if(state.add(originalTime - minTime)){
                for (ClusterContainer container : containers) {
                    queue.add(container.id);
                }
                insertClusters(containers, bestChunk);
            }

            if(j % 100 == 99){
                State dump = state.dump();
                dump.print(j+1);
                dumps.add(dump);
            }
        }
        System.out.println("End Optimiser");
        Set<Cluster> result = new HashSet<>();
        for (ClusterContainer container : clusters) {
            result.add(container.getCluster());
        }
        return result;
    }

    /**
     * Exchange old clusters with new versions and update neighbor and own positions
     */
    private void insertClusters(Collection<ClusterContainer> containers, Chunk chunk){
        Set<Point> updatePositions = new HashSet<>();
        Iterator<Cluster> iterator = chunk.clusters.iterator();
        for (ClusterContainer oldContainer : containers.stream().sorted().toList()) {
            updatePositions.addAll(oldContainer.getNeighborPositions());
            updatePositions.addAll(List.of(oldContainer.getCluster().getPoints()));
            this.clusters[oldContainer.id] = new ClusterContainer(iterator.next(), oldContainer.id);
            oldContainer.invalidate();
        }
        updateNeighbors(updatePositions);
    }

    private static class State {
        private String pathTime;
        private String clusterTime;
        private int ties;
        private int fails;
        private int successes;
        private int expense;
        private int improvement;

        public State() {
        }

        private State(String pathTime, String clusterTime, int ties, int fails, int successes, int expense, int improvement) {
            this.pathTime = pathTime;
            this.clusterTime = clusterTime;
            this.ties = ties;
            this.fails = fails;
            this.successes = successes;
            this.expense  =expense;
            this.improvement = improvement;
        }

        public void recordPath(String time){
            pathTime=time;
        }

        public void recordCluster(String time){
            clusterTime=time;
        }

        public boolean add(int diff){
            if (diff == 0) {
                tie();
            } else if (diff > 0) {
                success(-diff);
                return true;
            } else {
                fail(-diff);
            }
            return false;
        }

        private void fail(int expense){
            this.expense+=expense;
            fails++;
        }

        private void tie(){
            ties++;
        }

        private void success(int profit){
            successes++;
            improvement+=profit;
        }

        public State dump(){
            return new State(
                    pathTime,
                    clusterTime,
                    ties,
                    fails,
                    successes,
                    expense,
                    improvement
            );
        }

        public void print(int n) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format(
                    "///////////////////n=%05d///////////////////", n
                    )
            ).append("\n");
            builder.append("Path Time   : ").append(pathTime).append("\n");
            builder.append("Cluster Time: ").append(clusterTime).append("\n");
            builder.append("---------------------------------------------").append("\n");
            builder.append("Successes   : ").append(successes).append("\n");
            builder.append("Ties        : ").append(ties).append("\n");
            builder.append("Fails       : ").append(fails).append("\n");
            builder.append("Sum         : ").append(successes + ties + fails).append("\n");
            builder.append("---------------------------------------------").append("\n");
            builder.append("Improvement : ").append(improvement).append("\n");
            builder.append("Expense     : ").append(expense).append("\n");
            builder.append("/////////////////////////////////////////////");
            System.out.println(builder);
        }
    }

    /**
     * Contains the clusters on this field.
     */
    private static class ClusterContainer implements Comparable<ClusterContainer>{
        private final int id;
        private final Set<Point> neighborPositions;

        private final Set<Integer> neighborClusters;
        private final Cluster cluster;
        private boolean invalid;

        public ClusterContainer(Cluster cluster, int id) {
            this.cluster = cluster;
            this.id = id;
            this.neighborClusters = new HashSet<>();
            this.neighborPositions = new HashSet<>();
            this.invalid = false;
        }

        public boolean isInvalid() {
            return invalid;
        }

        public void invalidate() {
            this.invalid = true;
        }

        @Override
        public int compareTo(ClusterContainer o) {
            return cluster.compareTo(o.getCluster());
        }

        public int getId() {
            return id;
        }

        public Cluster getCluster() {
            return cluster;
        }

        public void addNeighbor(ClusterContainer neighbor, Point pos) {
            neighborPositions.add(pos);
            neighborClusters.add(neighbor.getId());
        }

        public Collection<Integer> getNeighbors() {
            return neighborClusters;
        }

        public Collection<Point> getNeighborPositions() {
            return neighborPositions;
        }
    }

    private static class Queue {
        private final PriorityQueue<ClusterContainer> queue = new PriorityQueue<>();
        private final ClusterOptimiser optimiser;
        private final Set<Integer> next = new HashSet<>();

        public Queue(ClusterOptimiser optimiser) {
            this.optimiser = optimiser;
            queue.addAll(List.of(optimiser.clusters));
        }

        public void add(int id){
            next.add(id);
        }

        public ClusterContainer pop(){
            ClusterContainer result = queue.peek();
            while(result != null) {
                if (!result.isInvalid()) {
                    return queue.poll();
                }
                queue.poll();
                result = queue.peek();
            }
            if(queue.isEmpty()){
                queue.addAll(next.stream().map((id) -> optimiser.clusters[id]).toList());
                next.clear();
            }
            return queue.poll();
        }

        public void reset(ClusterContainer[] clusters) {
            queue.addAll(Arrays.stream(clusters).toList());
        }
    }

    private static class OptimiserVariant {
        private final CompositorSeason season;
        private final TimeLookup lookup;
        private final PathStore store;
        private final Point[] positionByIndex;

        public OptimiserVariant(CompositorSeason season, Collection<Cluster> origin, PathStore store) {
            this.season = season;
            this.store = store;
            this.lookup = store.getLookup();
            this.positionByIndex = new Point[origin.size() * 3];
            int index = 0;
            for (Cluster cluster : origin) {
                for (Point pos : cluster.getPoints()) {
                    this.positionByIndex[index++] = pos;
                }
            }
        }

        public Set<Cluster> createClusters(Genome genome) {
            Set<Cluster> elements = new HashSet<>();
            int[] chromosomes = genome.getChromosomes();
            for (int i = 0; i < genome.getChromosomes().length / 3; i++) {
                Point a = positionByIndex[chromosomes[i * 3]];
                Point b = positionByIndex[chromosomes[i * 3 + 1]];
                Point c = positionByIndex[chromosomes[i * 3 + 2]];
                elements.add(new Cluster(
                        store.getPathToFactory(a).invert(),
                        store.getPathToFactory(c),
                        season.getPathTo(a, b),
                        season.getPathTo(b, c)));
            }
            return elements;
        }

        public int getTime(int from, int to, Genome genome) {
            int a = genome.getChromosomes()[from];
            int b = genome.getChromosomes()[to];
            return lookup.getEdge(positionByIndex[a], positionByIndex[b]);
        }

        public int getFactoryTime(int pos, Genome genome) {
            int a = genome.getChromosomes()[pos];
            return lookup.toFactory(positionByIndex[a]);
        }
    }

    private static class GeneticCluster extends GeneticSystem {
        private final OptimiserVariant variant;
        private final int[] cache;

        public GeneticCluster(int amount, float elitism, OptimiserVariant variant) {
            super(amount, variant.positionByIndex.length, elitism);
            this.variant = variant;
            this.cache = new int[(1 << 12) - 1];
            initPopulation();
        }

        private int createIndex(Genome genome, int i) {
            int a = genome.getChromosomes()[i];
            int b = genome.getChromosomes()[i + 1];
            int c = genome.getChromosomes()[i + 2];
            return (a & 0xF) << 8 | (b & 0xF) << 4 | c & 0xF;
        }

        @Override
        protected float getFitness(Genome genome) {
            int time = 0;
            for (int i = 0; i < genomeSize / 3; i++) {
                int index = i * 3;
                int z = createIndex(genome, index);
                //TODO: Useful ?
                if (cache[z] == 0) {
                    cache[z] = variant.getFactoryTime(index, genome) +
                            variant.getTime(index, index + 1, genome) +
                            variant.getTime(index + 1, index + 2, genome) +
                            variant.getFactoryTime(index + 2, genome);
                }
                time += cache[z];
            }
            return 1f / time;
        }
    }
}
