package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicy;

/**
 * Verifies a single {@link FilterService} is safe to share across threads (issue #381): concurrent
 * redaction on one instance produces consistent output, and the filter set is built once.
 */
public class FilterServiceConcurrencyTest {

    @Test
    public void sharedInstanceFiltersConcurrently() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());
        final ContextService contextService = new DefaultContextService();
        final VectorService vectorService = new InMemoryVectorService();

        // One shared instance, as a per-row Spark/Kafka UDF would use it.
        final PlainTextFilterService service =
                new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);

        final Policy policy = getPolicy();
        final String input = "My email is test@something.com and cc is 4121742025464465";
        final String expected = "My email is {{{REDACTED-email-address}}} and cc is {{{REDACTED-credit-card}}}";

        final int threads = 16;
        final int iterationsPerThread = 8;
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final CopyOnWriteArrayList<Throwable> failures = new CopyOnWriteArrayList<>();
        final AtomicInteger mismatches = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        final TextFilterResult result = service.filter(policy, "context", input);
                        if (!expected.equals(result.getFilteredText())) {
                            mismatches.incrementAndGet();
                        }
                    }
                } catch (final Throwable e) {
                    failures.add(e);
                }
            });
        }

        ready.await();          // all threads parked at the gate
        start.countDown();      // release them together to maximize the cold-cache race
        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS), "filtering threads did not finish");

        Assertions.assertTrue(failures.isEmpty(), () -> "concurrent filter() threw: " + failures);
        Assertions.assertEquals(0, mismatches.get(), "concurrent filter() produced inconsistent output");

        // The policy fully determines the filter set, so a shared instance ends with exactly one
        // cached entry, built once via computeIfAbsent rather than once per racing thread.
        Assertions.assertEquals(1, service.filterCache.size());
    }

}
