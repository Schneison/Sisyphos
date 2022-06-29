import java.util.*;
import java.util.stream.IntStream;

public abstract class GeneticSystem {
    protected final Population population;
    protected final int genomeSize;

    protected GeneticSystem(int amount, int genomeSize, float elitism) {
        this.genomeSize = genomeSize;
        this.population = new Population(amount, (int) (amount * elitism));
    }

    protected void initPopulation() {
        Entity[] entities = population.entities;
        int[] data = IntStream.range(0, genomeSize).toArray();
        for (int i = 0; i < entities.length; i++) {
            entities[i] = new Entity(new Genome(MathUtil.shuffle(data)), this);
        }
    }

    public Genome run(int generations, Random rand) {
        for (int current = 0; current < generations; current++) {
            population.nextGeneration(rand);
        }
        ValueKeyPair<Entity> pair = population.calculateFitnessSum().poll();
        if (pair == null) {
            throw new IllegalStateException();
        }
        return pair.getValue().getDna();
    }

    protected abstract float getFitness(Genome genome);

    private static class Population {
        private final int elitismCount;
        private Entity[] entities;
        private double fitnessSum;

        public Population(int amount, int elitismCount) {
            this.entities = new Entity[amount];
            this.elitismCount = elitismCount;
        }

        private static final PriorityQueue<ValueKeyPair<Entity>> bestEntities = new PriorityQueue<>(Comparator.reverseOrder());
        public PriorityQueue<ValueKeyPair<Entity>> calculateFitnessSum() {
            bestEntities.clear();
            fitnessSum = 0;
            for (Entity entity : entities) {
                fitnessSum += entity.getFitness();
                bestEntities.add(new ValueKeyPair<>(entity.getFitness(), entity));
            }
            return bestEntities;
        }

        public Entity selectParent(Random rand) {
            double value = rand.nextDouble(fitnessSum);
            double sum = 0;
            for (Entity entity : entities) {
                sum += entity.getFitness();
                if (sum >= value) {
                    return entity;
                }
            }
            throw new IllegalStateException();
        }

        public void nextGeneration(Random rand) {
            Entity[] newEntities = new Entity[entities.length];
            calculateFitnessSum();
            for (int i = 0; i < elitismCount; i++) {
                ValueKeyPair<Entity> pair = bestEntities.poll();
                if (pair == null) {
                    throw new IllegalStateException();
                }
                newEntities[i] = pair.getValue();
            }
            for (int i = elitismCount; i < newEntities.length; i += 2) {
                Entity a = selectParent(rand);
                Entity b = selectParent(rand);
                a.crossbreed(b, newEntities, i, rand);
            }
            this.entities = newEntities;
        }
    }

    private static class Entity {
        private final Genome genome;
        private final GeneticSystem system;
        private final float fitness;

        public Entity(Genome genome, GeneticSystem system) {
            this(genome, system, null);
        }

        public Entity(Genome genome, GeneticSystem system, Random rand) {
            this.genome = genome;
            this.system = system;
            if (rand != null) {
                genome.mutate(rand);
            }
            this.fitness = system.getFitness(genome);
        }

        private static final Genome[] offsprings = new Genome[2];
        public void crossbreed(Entity other, Entity[] newEntities, int index, Random rand) {
            genome.crossbreed(other.getDna(), rand, offsprings);
            newEntities[index] = new Entity(offsprings[0], system, rand);
            if (newEntities.length > index + 1) {
                newEntities[index + 1] = new Entity(offsprings[1], system, rand);
            }
        }

        public float getFitness() {
            return fitness;
        }

        public Genome getDna() {
            return genome;
        }
    }
}

