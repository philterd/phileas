package ai.philterd.phileas.model.utils;

import org.apache.commons.codec.digest.MurmurHash3;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.Collection;
import java.util.function.Function;

import static org.apache.commons.codec.digest.MurmurHash3.DEFAULT_SEED;

public class BloomFilter<T> {

    private final BitSet bitSet;
    private final Function<T, Integer>[] hashFunctions;

    public BloomFilter(int size) {
        this.bitSet = new BitSet(size);
        this.hashFunctions = createHashFunctions();
    }

    public BloomFilter(Collection<T> elements) {
        this.bitSet = new BitSet(elements.size());
        this.hashFunctions = createHashFunctions();
        putAll(elements);
    }

    public void put(T element) {
        for (final Function<T, Integer> hashFunction : hashFunctions) {
            final int hash = hashFunction.apply(element);
            bitSet.set(Math.abs(hash) % bitSet.size(), true);
        }
    }

    public void putAll(Collection<T> elements) {
        for(T element : elements) {

            for (final Function<T, Integer> hashFunction : hashFunctions) {
                final int hash = hashFunction.apply(element);
                bitSet.set(Math.abs(hash) % bitSet.size(), true);
            }
        }
    }

    public boolean mightContain(T element) {
        for (final Function<T, Integer> hashFunction : hashFunctions) {
            final int hash = hashFunction.apply(element);
            if (!bitSet.get(Math.abs(hash) % bitSet.size())) {
                return false;
            }
        }
        return true;
    }

    private Function<T, Integer>[] createHashFunctions() {

        Function<T, Integer>[] functions = new Function[2];

        functions[0] = (T element) -> {
            final byte[] data = element.toString().getBytes(StandardCharsets.UTF_8);
            return MurmurHash3.hash32x86(data, 0, data.length, DEFAULT_SEED);
        };

        functions[1] = (T element) -> {

            try {

                final MessageDigest digest = MessageDigest.getInstance("MD5");
                byte[] hash = digest.digest(element.toString().getBytes(StandardCharsets.UTF_8));

                int hashCode = 0;
                for (int i = 0; i < 4; i++) {
                    hashCode = (hashCode << 8) | (hash[i] & 0xFF);
                }

                return hashCode;

            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 algorithm not found", e);
            }

        };

        return functions;

    }

}
