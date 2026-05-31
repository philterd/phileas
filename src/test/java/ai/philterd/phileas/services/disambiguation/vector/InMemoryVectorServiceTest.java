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
package ai.philterd.phileas.services.disambiguation.vector;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class InMemoryVectorServiceTest {

    @Test
    public void concurrentInsertsDoNotLoseCounts() throws InterruptedException {

        // Documents are processed concurrently, so concurrent inserts into the same context must not
        // lose increments. The previous check-then-act increment dropped counts under contention.
        final InMemoryVectorService vectorService = new InMemoryVectorService();

        final int vectorSize = 16;
        final String context = "c";
        final int threads = 8;
        final int insertsPerThread = 5_000;
        final int index = 3;

        // Each insert sets exactly one index, so the final count at that index is fully determined.
        final double[] hashes = new double[vectorSize];
        hashes[index] = 1;
        final Span span = Span.make(0, 4, FilterType.SSN, context, 0.0, "x", "x", "",
                false, true, new String[]{"x"}, 0);

        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);

        for(int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    start.await();
                    for(int i = 0; i < insertsPerThread; i++) {
                        vectorService.hashAndInsert(context, hashes, span, vectorSize);
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        Assertions.assertTrue(done.await(30, TimeUnit.SECONDS), "inserts did not finish in time");
        executor.shutdownNow();

        final double count = vectorService.getVectorRepresentation(context, FilterType.SSN).get((double) index);
        Assertions.assertEquals((double) threads * insertsPerThread, count,
                "every concurrent insert should be counted exactly once");
    }

    @Test
    public void vectorsAreIsolatedPerContext() {

        // The cache is keyed by context; an insert into one context must not appear in another.
        final InMemoryVectorService vectorService = new InMemoryVectorService();

        final double[] hashes = new double[16];
        hashes[2] = 1;
        final Span span = Span.make(0, 4, FilterType.SSN, "a", 0.0, "x", "x", "",
                false, true, new String[]{"x"}, 0);

        vectorService.hashAndInsert("a", hashes, span, 16);

        Assertions.assertEquals(1.0, vectorService.getVectorRepresentation("a", FilterType.SSN).get(2.0),
                "the insert should be visible in its own context");
        Assertions.assertTrue(vectorService.getVectorRepresentation("b", FilterType.SSN).isEmpty(),
                "a different context must not see the insert");
    }

}
