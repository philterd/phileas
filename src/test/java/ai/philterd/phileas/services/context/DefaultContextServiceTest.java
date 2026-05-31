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
package ai.philterd.phileas.services.context;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultContextServiceTest {

    @Test
    public void putAndGetRoundTrips() {
        final DefaultContextService service = new DefaultContextService();
        Assertions.assertFalse(service.containsToken("token"));
        service.putReplacement("token", "replacement", "filter-type");
        Assertions.assertTrue(service.containsToken("token"));
        Assertions.assertEquals("replacement", service.getReplacement("token"));
        Assertions.assertTrue(service.containsReplacement("replacement"));
    }

    @Test
    public void computeReplacementIfAbsentGeneratesThenReuses() {
        final DefaultContextService service = new DefaultContextService();
        final AtomicInteger supplierCalls = new AtomicInteger();

        final String first = service.computeReplacementIfAbsent("token", "filter-type", () -> {
            supplierCalls.incrementAndGet();
            return "generated";
        });
        final String second = service.computeReplacementIfAbsent("token", "filter-type", () -> {
            supplierCalls.incrementAndGet();
            return "should-not-be-used";
        });

        Assertions.assertEquals("generated", first);
        Assertions.assertEquals("generated", second, "an existing replacement must be reused");
        Assertions.assertEquals(1, supplierCalls.get(), "the supplier must run only on the first call");
    }

    /**
     * Guards finding #2: under concurrency the old contains/get/else-generate-put sequence let
     * multiple threads each generate a replacement for the same token. computeReplacementIfAbsent
     * must invoke the supplier exactly once and hand every thread the identical replacement.
     */
    @Test
    public void computeReplacementIfAbsentIsAtomicUnderContention() throws Exception {
        final DefaultContextService service = new DefaultContextService();
        final int threads = 32;
        final AtomicInteger supplierCalls = new AtomicInteger();
        final Set<String> observed = Collections.newSetFromMap(new ConcurrentHashMap<>());
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        final ExecutorService pool = Executors.newFixedThreadPool(threads);

        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        final String replacement = service.computeReplacementIfAbsent("ssn", "ssn", () -> {
                            supplierCalls.incrementAndGet();
                            return "REDACTED-" + System.nanoTime();
                        });
                        observed.add(replacement);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            Assertions.assertTrue(done.await(30, TimeUnit.SECONDS), "threads did not finish in time");
        } finally {
            pool.shutdownNow();
        }

        Assertions.assertEquals(1, supplierCalls.get(), "the replacement must be generated exactly once");
        Assertions.assertEquals(1, observed.size(), "every thread must observe the same replacement");
    }

    /**
     * Guards finding #1: concurrent writes of distinct tokens must not lose entries or corrupt
     * the backing map (a plain HashMap can drop entries or spin on concurrent resize).
     */
    @Test
    public void concurrentPutsOfDistinctTokensDoNotLoseEntries() throws Exception {
        final DefaultContextService service = new DefaultContextService();
        final int threads = 8;
        final int perThread = 5000;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        final ExecutorService pool = Executors.newFixedThreadPool(threads);

        try {
            for (int t = 0; t < threads; t++) {
                final int threadId = t;
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            final String token = "token-" + threadId + "-" + i;
                            service.putReplacement(token, "replacement-" + threadId + "-" + i, "filter-type");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            Assertions.assertTrue(done.await(30, TimeUnit.SECONDS), "threads did not finish in time");
        } finally {
            pool.shutdownNow();
        }

        for (int t = 0; t < threads; t++) {
            for (int i = 0; i < perThread; i++) {
                final String token = "token-" + t + "-" + i;
                Assertions.assertEquals("replacement-" + t + "-" + i, service.getReplacement(token),
                        "entry was lost or corrupted under concurrent writes: " + token);
            }
        }
    }

}
