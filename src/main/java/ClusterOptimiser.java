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
            }//-6301
            /*Scope scope = new Scope(random);
            scope.tryLoad(rand, (9 + 3 * factor));*/
            Cluster cluster = random.getCluster();
            Set<ClusterContainer> containers = new HashSet<>();
            Set<Cluster> currentClusters = new HashSet<>();
            List<Point> positions = new ArrayList<>(Arrays.asList(cluster.getPoints()));
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
                    positions,
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

    private class Scope {
        private final ClusterContainer main;

        private final Set<ClusterContainer> containers = new HashSet<>();
        private final Set<Cluster> clusters = new HashSet<>();
        private final List<Point> positions = new ArrayList<>();
        private int originalTime;

        public Scope(ClusterContainer main) {
            this.main = main;
            positions.addAll(Arrays.asList(main.getCluster().getPoints()));
            clusters.add(main.getCluster());
            containers.add(main);
        }

        public void tryLoad(Random rand, int maxSize){
            List<Integer> neighbors = new ArrayList<>(main.getNeighbors());
            //Shuffle so we don't test the same neighbors every run
            Collections.shuffle(neighbors, rand);

            // Only one "special" (cluster with less than three positions) cluster is allowed due to the construction from the genome
            boolean special = false;
            int size = 0;
            for (int id : neighbors) {
                ClusterContainer container = getContainer(id);
                Cluster cluster = container.getCluster();
                if(!cluster.isNormal()){
                    if(special) {
                        continue;
                    }
                    special = true;
                }
                size+=3;//Always add 3, so we don't go over maxSize if we have a special cluster
                positions.addAll(Arrays.asList(cluster.getPoints()));
                /*if(!positions.addAll(Arrays.asList(cluster.getPoints()))){
                    // Cluster compliantly contained in other cluster, possibly 2 valued Material
                    continue;
                }*/
                clusters.add(cluster);
                containers.add(container);
                //We don't look at 2 valued Materials or single routes
                /*if (positions.size() % 3 != 0 || cluster.getPoints().length % 3 != 0) {
                    return false;
                }*/
                // Check if scope is full
                if (size >= maxSize) {
                    break;
                }
            }
            originalTime = Cluster.sumChunkTime(
                    containers.stream().map(ClusterContainer::getCluster)
            );
        }
    }

    private static class OptimiserVariant {
        private final CompositorSeason season;
        private final TimeLookup lookup;
        private final PathStore store;
        private final Point[] positionByIndex;

        public OptimiserVariant(CompositorSeason season, List<Point> positions, PathStore store) {
            this.season = season;
            this.store = store;
            this.lookup = store.getLookup();
            this.positionByIndex = new Point[positions.size()];
            int index = 0;
            for (Point pos : positions) {
                this.positionByIndex[index++] = pos;
            }
        }

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
            for (int i = 0; i < genome.getLength() / 3; i++) {
                int index = i * 3;
               elements.add(toCluster(genome, index));
//                Point a = positionByIndex[chromosomes[index]];
//                Point b = positionByIndex[chromosomes[index + 1]];
//                Point c = positionByIndex[chromosomes[index + 2]];
//                elements.add(new Cluster(
//                        store.getPathToFactory(a).invert(),
//                        store.getPathToFactory(c),
//                        season.getPathTo(a, b),
//                        season.getPathTo(b, c)));
            }
            return elements;
        }

        private int toTime(Genome genome, int index){
            //Cluster with same origin and destination, no material
            if(index + 1 == genome.getLength()) {
                return getFactoryTime(genome, index) *2;
            }
            //Cluster with different origin and destination, no material
            else if(index + 2 == genome.getLength()){
                return getFactoryTime(genome, index) +
                        getTime(genome, index, index + 1) +
                        getFactoryTime(genome, index + 1);
            }
            return getFactoryTime(genome, index) +
                    getTime(genome, index, index + 1) +
                    getTime(genome, index + 1, index + 2) +
                    getFactoryTime(genome, index + 2);
        }

        private Cluster toCluster(Genome genome, int index){
            Point origin = getPos(genome, index);
            Point material = getPos(genome, index + 1);
            Point destination = getPos(genome, index + 2);
            //Cluster with same origin and destination, no material
            if(material == null) {
                return new Cluster(
                        store.getPathToFactory(origin).invert(),
                        store.getPathToFactory(origin));
            }
            //Cluster with different origin and destination, no material
            else if(destination == null){
                return new Cluster(
                        store.getPathToFactory(origin).invert(),
                        store.getPathToFactory(material),
                        season.getPathTo(origin, material));
            }
            return new Cluster(
                    store.getPathToFactory(origin).invert(),
                    store.getPathToFactory(destination),
                    season.getPathTo(origin, material),
                    season.getPathTo(material, destination));
        }

        private Point getPos(Genome genome, int index){
            if(genome.getChromosomes().length <= index){
                // If last chromosome represents a cluster which contains only 1 or 2 positions
                return null;
            }
            int posIndex = genome.getChromosomes()[index];
            return positionByIndex[posIndex];
        }

        /**
         * Retrieves the time from the first index to the second index.
         *
         * @param from Index of the first position
         * @param to   Index of the second position
         */
        public int getTime(Genome genome, int from, int to) {
            return lookup.getEdge(getPos(genome, from), getPos(genome, to));
        }

        /**
         * Retrieves the time from the index to the factory.
         *
         * @param index Index of the material position
         */
        public int getFactoryTime(Genome genome, int index) {
            return lookup.toFactory(getPos(genome, index));
        }
    }

    private static class GeneticCluster extends GeneticSystem {
        private final OptimiserVariant variant;
        private final int[] cache;

        public GeneticCluster(int amount, float elitism, OptimiserVariant variant) {
            super(amount, variant.positionByIndex.length, elitism);
            this.variant = variant;
            this.cache = new int[(1 << 15) - 1];
            initPopulation();
        }

        private int createIndex(int[] chromosomes, int i) {
            int a = chromosomes[i];
            int b = chromosomes.length > i + 1 ? chromosomes[i + 1] : 0x1F; // 31 as default unused value, so all bits are set, there should be no situation where
            int c = chromosomes.length > i + 2 ? chromosomes[i + 2] : 0x1F;
            return (a & 0x1F) << 10 | (b & 0x1F) << 5 | c & 0x1F;
        }

        @Override
        protected float getFitness(Genome genome) {
            int time = 0;
            for (int i = 0; i < genomeSize / 3; i++) {
                int index = i * 3;
                int z = createIndex(genome.getChromosomes(), index);
                //TODO: Useful ?
                if (cache[z] == 0) {
                    cache[z] = variant.toTime(genome, index);
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
