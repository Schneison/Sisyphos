import com.google.common.base.Stopwatch;

import java.util.*;

/**
 * Optimises the clusters found by the {@link ClusterCompositor} beforehand. For the optimisation a genetic algorithm
 * is used.
 */
public class ClusterOptimiser {
    private final Queue queue;
    private final Deque<State> dumps = new ArrayDeque<>();
    private final Map<Point, Integer> clusterByPos;
    private final ClusterContainer[] clusters;
    private final PathStore store;
    private final State state;

    public ClusterOptimiser(Set<Cluster> clusters, Environment env) {
        this.clusterByPos = new HashMap<>();
        this.state = new State();
        this.store = env.getStore();
        final int[] id = {0};
        this.clusters = new ClusterContainer[clusters.size()];
        clusters.stream().sorted(Cluster::compareTo).forEach(cluster->{
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

    /**
     * Starts the optimisation.

     * @param config Configuration used for pathfinding if any is needed.
     *
     * @return The optimised clusters
     */
    public Set<Cluster> process(PathStore.Config config) {
        Random rand = new Random(42);
        Stopwatch pathWatch = Stopwatch.createUnstarted();
        Stopwatch clusterWatch = Stopwatch.createUnstarted();
        Log.info("Start Optimiser");
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
            Set<Cluster> currentClusters = new HashSet<>();
            Set<Point> positions = new HashSet<>(Arrays.asList(cluster.getPoints()));
            if (positions.size() % 3 != 0) {
                continue;
            }
            currentClusters.add(cluster);
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
                currentClusters.add(cluster);
                containers.add(container);
                //We don't look at 2 valued Materials or single routes
                if (positions.size() % 3 != 0 || cluster.getPoints().length % 3 != 0) {
                    continue o;
                }
                if (positions.size() >= (12 + 3 * factor)) {
                    break;
                }
            }
            int originalTime = Cluster.sumChunkTime(currentClusters);
            Chunk bestChunk = null;
            int minTime = Integer.MAX_VALUE;
            pathWatch.start();
            OptimiserVariant variant = new OptimiserVariant(
                    CompositorSeason.fromRange(store, positions, config),
                    currentClusters,
                    store
            );
            pathWatch.stop();
            //Update Path finding time
            state.recordPath(pathWatch.toString());
            clusterWatch.start();

            for (int i = 0; i < 14; i++) {
                //Create cluster Combination simulation
                GeneticCluster geneticCluster = new GeneticCluster(150 + factor * 50, 0.025f, variant);
                //Run simulation
                Genome genome = geneticCluster.run(120 + factor * 60, rand);
                Chunk result = new Chunk(variant.createClusters(genome));
                //Check if the best found chunk is better that the already found one
                if (result.getTime() < minTime) {
                    minTime = result.getTime();
                    bestChunk = result;//-16249 - 6:10
                }
            }
            // Should only happen if every cluster combination has a time bigger that Integer.MAX_VALUE, which should
            // be impossible
            if (bestChunk == null) {
                throw new IllegalStateException();
            }
            clusterWatch.stop();
            //Update Cluster time
            state.recordCluster(clusterWatch.toString());

            //Add to current state and update queue if we changed the clusters
            if(state.add(originalTime - minTime)){
                for (ClusterContainer container : containers) {
                    queue.add(container.id);
                }
                insertClusters(containers, bestChunk);
            }

            //Check the state all 100 iterations
            if(j % 100 == 99){
                State oldState = dumps.peekLast();
                State dump = state.dump();
                dump.print(j+1);
                dumps.add(dump);
                if(oldState != null && oldState.successes == dump.successes){
                    break;
                }
            }
        }
        Log.info("End Optimiser");
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
        Iterator<Cluster> iterator = chunk.getClusters().iterator();
        for (ClusterContainer oldContainer : containers.stream().sorted().toList()) {
            updatePositions.addAll(oldContainer.getNeighborPositions());
            updatePositions.addAll(List.of(oldContainer.getCluster().getPoints()));
            this.clusters[oldContainer.id] = new ClusterContainer(iterator.next(), oldContainer.id);
            oldContainer.invalidate();
        }
        updateNeighbors(updatePositions);
    }

    /**
     * Helper class to summarise the result of the current optimisation.
     */
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

        /**
         * Increase the fail count and add the given expense.
         *
         * @param expense Time difference between the found chunk and the old one.
         */
        private void fail(int expense){
            this.expense+=expense;
            fails++;
        }

        /**
         * Increase the tie count.
         */
        private void tie(){
            ties++;
        }

        /**
         * Increase the success count and add the given profit.
         *
         * @param profit Time difference between the found chunk and the old one.
         */
        private void success(int profit){
            successes++;
            improvement+=profit;
        }

        /**
         * Creates a copy of the current state.
         */
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

        /**
         * Prints this state to the console.
         *
         * @param n Current index of optimisation
         */
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
            Log.info(builder);
        }
    }


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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClusterContainer)) return false;
            ClusterContainer container = (ClusterContainer) o;
            return getId() == container.getId() && getCluster().equals(container.getCluster());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId(), getCluster());
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
                queue.addAll(next.stream().map(id -> optimiser.clusters[id]).toList());
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

        /**
         * Create the clusters from the given genetic data.
         */
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

        /**
         * Retrieves the time from the first index to the second index.
         *
         * @param from Index of the first position
         * @param to Index of the second position
         */
        public int getTime(int from, int to, Genome genome) {
            int a = genome.getChromosomes()[from];
            int b = genome.getChromosomes()[to];
            return lookup.getEdge(positionByIndex[a], positionByIndex[b]);
        }

        /**
         * Retrieves the time from the index to the factory.
         *
         * @param pos Index of the material position
         */
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
            if(time == 0){
                throw new IllegalStateException();
            }
            return 1f / time;
        }
    }
}
