import java.util.Map;

public class ValueKeyPair<T> implements Comparable<ValueKeyPair<T>> {
    private final float key;
    private final T value;

    public ValueKeyPair(float key, T value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public int compareTo(ValueKeyPair o) {
        return Float.compare(key, o.getKey());
    }

    public float getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }
}
