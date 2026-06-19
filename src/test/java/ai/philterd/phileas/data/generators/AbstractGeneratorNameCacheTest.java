package ai.philterd.phileas.data.generators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Verifies resource-backed name lists are loaded once and shared (issue #383): generators loading the
 * same resource reuse one immutable list instead of each duplicating it, on the replacement path.
 */
public class AbstractGeneratorNameCacheTest {

    // Minimal concrete subclass to reach the protected loadNames.
    private static final class TestGenerator extends AbstractGenerator<String> {
        @Override public String random() {
            return null;
        }
        @Override public long poolSize() {
            return 0;
        }
        List<String> load(final String path) throws Exception {
            return loadNames(path);
        }
    }

    @Test
    public void nameListsAreSharedAcrossLoads() throws Exception {
        final TestGenerator a = new TestGenerator();
        final TestGenerator b = new TestGenerator();

        // Two generators loading the same resource share one immutable list, not a copy each.
        Assertions.assertSame(a.load("/first-names.txt"), b.load("/first-names.txt"));
        Assertions.assertFalse(a.load("/first-names.txt").isEmpty());
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> a.load("/first-names.txt").add("x"));
    }

    @Test
    public void concurrentLoadsShareOneListAndReadsAreSafe() throws Exception {
        final int threads = 16;
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final CopyOnWriteArrayList<List<String>> results = new CopyOnWriteArrayList<>();
        final CopyOnWriteArrayList<Throwable> failures = new CopyOnWriteArrayList<>();
        final TestGenerator generator = new TestGenerator();

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    final List<String> list = generator.load("/surnames.txt");
                    list.contains("smith"); // concurrent read
                    results.add(list);
                } catch (final Throwable e) {
                    failures.add(e);
                }
            });
        }

        ready.await();          // all threads parked at the gate
        start.countDown();      // release together
        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS), "threads did not finish");

        Assertions.assertTrue(failures.isEmpty(), () -> "concurrent load threw: " + failures);
        Assertions.assertEquals(threads, results.size());
        // Every thread observed the same shared list (no duplication, no torn read).
        final List<String> first = results.get(0);
        Assertions.assertTrue(results.stream().allMatch(r -> r == first));
    }

}
