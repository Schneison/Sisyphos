import java.util.Objects;

/**
 * Helper class which represents a value key pair.
 *
 * @param <T> Type of the value.
 */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValueKeyPair<?> that)) return false;
        return Float.compare(that.getKey(), getKey()) == 0 && getValue().equals(that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getValue());
    }

    public float getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }
}
