import java.util.BitSet;
import java.util.Random;

public class Genome {
    private final int[] chromosomes;

    public Genome(int[] chromosomes) {
        this.chromosomes = chromosomes;
    }

    // DM, IVM and ISM
    public void mutate(Random rand) {
        if (rand.nextFloat() <= 0.025) {
            exchangeMutation(rand);
        }
    }

    // EM - bad
    public void exchangeMutation(Random rand) {
        MathUtil.swap(chromosomes,
                rand.nextInt(0, chromosomes.length),
                rand.nextInt(0, chromosomes.length - 1)
        );
    }

    //DM
    private void displacementMutation(Random rand) {

    }

    // IVM
    private void inversionMutation(Random rand) {
    }

    // ISM
    private void insertionMutation(Random rand) {
        int a = rand.nextInt(0, chromosomes.length);
        int b = rand.nextInt(0, chromosomes.length - 1);
        for (int i = a; i < b; i++) {
            MathUtil.swap(chromosomes, i, i + 1);
        }
    }

    public int getLength() {
        return chromosomes.length;
    }

    public int[] getChromosomes() {
        return chromosomes;
    }

    private static final int OFFSET = 0;
    //TODO: replace with bit shift and bit mask
    private final static BitSet lookupA = new BitSet(16);
    private final static BitSet lookupB = new BitSet(16);
    public void orderedCrossover(Genome other, Random rand, Genome[] offsprings) {
        lookupA.clear();
        lookupB.clear();
        int size = getLength();
        int n1 = rand.nextInt(0, size - 1);
        int n2 = rand.nextInt(0, size);

        int start = Math.min(n1, n2);
        int end = Math.max(n1, n2);
        int[] childA = new int[size];
        int[] childB = new int[size];
        for (int i = start; i < end; i++) {
            childA[i] = chromosomes[i];
            childB[i] = other.chromosomes[i];
            lookupA.set(chromosomes[i] - OFFSET, true);
            lookupB.set(other.chromosomes[i] - OFFSET, true);
        }

        int indexA = 0;
        int indexB = 0;
        for (int i = 0; i < size; i++) {
            int index = (end + i) % size;
            if (index == start && i > 0) {
                break;
            }
            int v;
            int y;
            //Search for next chromosome which is not already used, start at the end of the cut point
            for (; lookupA.get(v = other.chromosomes[(end + indexA) % size] - OFFSET); indexA++) {
            }
            for (; lookupB.get(y = chromosomes[(end + indexB) % size] - OFFSET); indexB++) {
            }
            int a = v + OFFSET;
            int b = y + OFFSET;
            childA[index] = a;
            childB[index] = b;
            lookupA.set(a - OFFSET, true);
            lookupB.set(b - OFFSET, true);
        }
        offsprings[0] = new Genome(childA);
        offsprings[1] = new Genome(childB);
    }

    // ER, OX1, POS and OX2
    public void crossbreed(Genome other, Random rand, Genome[] offsprings) {
        orderedCrossover(other, rand, offsprings);
    }

}
