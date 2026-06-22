/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicy;

/**
 * Verifies the warm, reusable filter-service API (issue #413): a single instance whose context and
 * vector services are supplied per call produces the same result as a service constructed per request
 * with those same services baked in, and one warm instance can serve requests that each bring their
 * own {@link ContextService} and {@link VectorService} without rebuilding its filter caches.
 */
public class WarmReuseTest {

    private static final String INPUT = "My email is test@something.com and cc is 4121742025464465";
    private static final String EXPECTED = "My email is {{{REDACTED-email-address}}} and cc is {{{REDACTED-credit-card}}}";

    private PhileasConfiguration configuration() {
        return new PhileasConfiguration(new Properties());
    }

    @Test
    public void perCallServicesMatchPerRequestConstruction() throws Exception {

        final Policy policy = getPolicy();

        // The per-request style: a fresh service with the context and vector services baked in.
        final ContextService perRequestContext = new DefaultContextService();
        final VectorService perRequestVectors = new InMemoryVectorService();
        final TextFilterResult perRequest = new PlainTextFilterService(
                configuration(), perRequestContext, perRequestVectors, null)
                .filter(policy, "context", INPUT);

        // The warm style: one instance, services supplied per call.
        final PlainTextFilterService warm = new PlainTextFilterService(configuration(), null);
        final TextFilterResult perCall = warm.filter(policy,
                new DefaultContextService(), new InMemoryVectorService(), "context", INPUT);

        Assertions.assertEquals(EXPECTED, perRequest.getFilteredText());
        Assertions.assertEquals(perRequest.getFilteredText(), perCall.getFilteredText());
    }

    @Test
    public void oneWarmInstanceServesDifferentPerCallServices() throws Exception {

        final Policy policy = getPolicy();

        // A single warm instance, reused. Its filter and post-filter caches are populated on the first
        // call and reused on the second even though the second call brings a different context and
        // vector service.
        final PlainTextFilterService warm = new PlainTextFilterService(configuration(), null);

        final TextFilterResult first = warm.filter(policy,
                new DefaultContextService(), new InMemoryVectorService(), "context-a", INPUT);
        final TextFilterResult second = warm.filter(policy,
                new DefaultContextService(), new InMemoryVectorService(), "context-b", INPUT);

        Assertions.assertEquals(EXPECTED, first.getFilteredText());
        Assertions.assertEquals(EXPECTED, second.getFilteredText());
    }

    @Test
    public void preparedPolicyAcceptsPerCallServices() throws Exception {

        final PlainTextFilterService warm = new PlainTextFilterService(configuration(), null);
        final PlainTextFilterService.PreparedPolicy prepared = warm.prepare(getPolicy());

        // The same prepared policy serves two requests that each supply their own services.
        Assertions.assertEquals(EXPECTED, prepared.filter(
                new DefaultContextService(), new InMemoryVectorService(), "context-a", INPUT).getFilteredText());
        Assertions.assertEquals(EXPECTED, prepared.filter(
                new DefaultContextService(), new InMemoryVectorService(), "context-b", INPUT).getFilteredText());
    }

    @Test
    public void oneWarmInstanceIsSafeUnderConcurrentPerCallServices() throws Exception {

        // One shared warm instance, many threads, each thread bringing its OWN context and vector
        // service per call. This is the per-call analogue of FilterServiceConcurrencyTest: it proves
        // the instance carries no per-request mutable state (the only shared state is the concurrent
        // filter caches and the thread-safe SecureRandom), so concurrent callers neither corrupt the
        // cache nor see each other's context/vector state.
        final PlainTextFilterService service = new PlainTextFilterService(configuration(), null);
        final Policy policy = getPolicy();

        final int threads = 8;
        final int iterationsPerThread = 16;
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        final AtomicInteger mismatches = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    start.await();
                    // Each thread uses its own context and vector services for every call.
                    for (int i = 0; i < iterationsPerThread; i++) {
                        final TextFilterResult result = service.filter(policy,
                                new DefaultContextService(), new InMemoryVectorService(), "context", INPUT);
                        if (!EXPECTED.equals(result.getFilteredText())) {
                            mismatches.incrementAndGet();
                        }
                    }
                } catch (final Throwable ex) {
                    failures.add(ex);
                }
            });
        }

        start.countDown();
        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS), "filtering threads did not finish");

        Assertions.assertTrue(failures.isEmpty(), () -> "concurrent per-call filter() threw: " + failures);
        Assertions.assertEquals(0, mismatches.get(), "concurrent per-call filter() produced inconsistent output");

        // The policy's filters were built once and reused across every thread and call: the cache was
        // never rebuilt, which is the whole point of the warm instance.
        Assertions.assertEquals(1, service.filterCache.size(), "the filter cache should hold exactly one warm entry");
    }

}
