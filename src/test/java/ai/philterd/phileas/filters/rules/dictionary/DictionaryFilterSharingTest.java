package ai.philterd.phileas.filters.rules.dictionary;

import ai.philterd.phileas.model.filtering.FilterType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Verifies predefined dictionaries are loaded once and shared (issue #383): repeated loads (as
 * separate FilterService instances would trigger) return the same immutable instance rather than
 * duplicating the data, and the shared structures are read-only so concurrent use is safe.
 */
public class DictionaryFilterSharingTest {

    @Test
    public void predefinedDictionaryIsSharedAcrossLoads() throws Exception {
        final var first = DictionaryFilter.getPredefinedDictionary(FilterType.FIRST_NAME);
        final var second = DictionaryFilter.getPredefinedDictionary(FilterType.FIRST_NAME);

        // Same instances => loaded once and shared, not duplicated per FilterService instance.
        Assertions.assertSame(first, second);
        Assertions.assertSame(first.lowerCaseTerms(), second.lowerCaseTerms());
        Assertions.assertSame(first.data(), second.data());
        Assertions.assertFalse(first.lowerCaseTerms().isEmpty());
    }

    @Test
    public void sharedDictionaryIsReadOnly() throws Exception {
        final var dictionary = DictionaryFilter.getPredefinedDictionary(FilterType.SURNAME);
        // Read-only structures make concurrent sharing safe.
        Assertions.assertThrows(UnsupportedOperationException.class, () -> dictionary.lowerCaseTerms().add("x"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> dictionary.data().clear());
    }

    @Test
    public void differentTypesAreDistinct() throws Exception {
        Assertions.assertNotSame(
                DictionaryFilter.getPredefinedDictionary(FilterType.FIRST_NAME),
                DictionaryFilter.getPredefinedDictionary(FilterType.SURNAME));
    }

    @Test
    public void concurrentAccessSharesOneInstanceAndReadsAreSafe() throws Exception {
        final int threads = 16;
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final CopyOnWriteArrayList<Object> results = new CopyOnWriteArrayList<>();
        final CopyOnWriteArrayList<Throwable> failures = new CopyOnWriteArrayList<>();

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    final var dictionary = DictionaryFilter.getPredefinedDictionary(FilterType.FIRST_NAME);
                    // Exercise concurrent reads of the shared structures.
                    dictionary.lowerCaseTerms().contains("john");
                    dictionary.data().size();
                    results.add(dictionary);
                } catch (final Throwable e) {
                    failures.add(e);
                }
            });
        }

        ready.await();          // all threads parked at the gate
        start.countDown();      // release together
        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS), "threads did not finish");

        Assertions.assertTrue(failures.isEmpty(), () -> "concurrent access threw: " + failures);
        Assertions.assertEquals(threads, results.size());
        // Every thread observed the same shared instance (no duplication, no torn read).
        final Object first = results.get(0);
        Assertions.assertTrue(results.stream().allMatch(r -> r == first));
    }

}
