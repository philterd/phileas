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

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FileBasedVectorServiceTest {

    private static final int VECTOR_SIZE = 64;
    private static final String ALGORITHM = "murmur3";

    private VectorBasedSpanDisambiguationService service(final VectorService vectorService) {
        final Properties properties = new Properties();
        properties.setProperty("span.disambiguation.enabled", "true");
        properties.setProperty("span.disambiguation.ignore.stopwords", "false");
        properties.setProperty("span.disambiguation.vector.size", "64");
        return new VectorBasedSpanDisambiguationService(new PhileasConfiguration(properties));
    }

    @Test
    public void missingFileIsAColdStart(@TempDir final Path dir) throws Exception {

        // Constructing against a path that does not exist must not fail; it is simply a cold start.
        final Path path = dir.resolve("does-not-exist.json");
        final FileBasedVectorService vectorService = new FileBasedVectorService(path, VECTOR_SIZE, ALGORITHM);

        Assertions.assertTrue(vectorService.getVectorRepresentation("c", FilterType.SSN).isEmpty(),
                "a missing persistence file should load no vectors");
    }

    @Test
    public void vectorsSurviveSaveAndReload(@TempDir final Path dir) throws Exception {

        final Path path = dir.resolve("vectors.json");
        final String context = "c";

        // Train PHONE_NUMBER on a phone-like context, then persist.
        final FileBasedVectorService original = new FileBasedVectorService(path, VECTOR_SIZE, ALGORITHM);
        final VectorBasedSpanDisambiguationService trainer = service(original);
        trainer.hashAndInsert(original, context, Span.make(0, 4, FilterType.PHONE_NUMBER, context, 0.0, "555-1212", "x", "",
                false, true, new String[]{"phone", "number", "call"}, 0));

        final Map<Double, Double> beforeSave = new HashMap<>(original.getVectorRepresentation(context, FilterType.PHONE_NUMBER));
        Assertions.assertFalse(beforeSave.isEmpty(), "training should have produced a vector");
        original.save();
        Assertions.assertTrue(Files.size(path) > 0, "the persistence file should have been written");

        // Reload into a brand new service and confirm the vectors are identical to what was saved.
        final FileBasedVectorService reloaded = new FileBasedVectorService(path, VECTOR_SIZE, ALGORITHM);
        Assertions.assertEquals(beforeSave, reloaded.getVectorRepresentation(context, FilterType.PHONE_NUMBER),
                "reloaded vectors should match what was persisted");

        // And the restored store still drives a correct decision: an ambiguous phone-context span
        // resolves to PHONE_NUMBER over the first-listed (untrained) SSN candidate.
        final VectorBasedSpanDisambiguationService afterRestart = service(reloaded);
        final List<FilterType> candidates = Arrays.asList(FilterType.SSN, FilterType.PHONE_NUMBER);
        final Span ambiguous = Span.make(0, 4, FilterType.SSN, context, 0.0, "123-4567", "x", "",
                false, true, new String[]{"phone", "number", "call"}, 0);

        Assertions.assertEquals(FilterType.PHONE_NUMBER, afterRestart.disambiguate(reloaded, context, candidates, ambiguous),
                "learning should survive a restart and still resolve the ambiguous value");
    }

    @Test
    public void accumulationContinuesAfterReload(@TempDir final Path dir) throws Exception {

        final Path path = dir.resolve("vectors.json");
        final String context = "c";

        final int index = 5;
        final double[] hashes = new double[64];
        hashes[index] = 1;
        final Span span = Span.make(0, 4, FilterType.SSN, context, 0.0, "x", "x", "",
                false, true, new String[]{"x"}, 0);

        // Accumulate twice, persist.
        final FileBasedVectorService original = new FileBasedVectorService(path, VECTOR_SIZE, ALGORITHM);
        original.hashAndInsert(context, hashes, span, 64);
        original.hashAndInsert(context, hashes, span, 64);
        original.save();

        // Reload and accumulate once more; the count must build on the persisted base (2 -> 3),
        // which also confirms the reloaded index map is mutable/thread-safe.
        final FileBasedVectorService reloaded = new FileBasedVectorService(path, VECTOR_SIZE, ALGORITHM);
        reloaded.hashAndInsert(context, hashes, span, 64);

        Assertions.assertEquals(3.0, reloaded.getVectorRepresentation(context, FilterType.SSN).get((double) index),
                "accumulation should continue from the persisted count after a reload");
    }

    @Test
    public void closeSavesTheVectors(@TempDir final Path dir) throws Exception {

        final Path path = dir.resolve("vectors.json");
        final String context = "c";

        try (final FileBasedVectorService vectorService = new FileBasedVectorService(path, VECTOR_SIZE, ALGORITHM)) {
            service(vectorService).hashAndInsert(vectorService, context, Span.make(0, 4, FilterType.SSN, context, 0.0, "x", "x", "",
                    false, true, new String[]{"social", "security"}, 0));
        }

        Assertions.assertTrue(Files.exists(path) && Files.size(path) > 0,
                "close() should have persisted the vectors");
        Assertions.assertFalse(new FileBasedVectorService(path, VECTOR_SIZE, ALGORITHM).getVectorRepresentation(context, FilterType.SSN).isEmpty(),
                "vectors persisted by close() should reload");
    }

    @Test
    public void vectorsBuiltWithADifferentVectorSizeAreDiscarded(@TempDir final Path dir) throws Exception {

        // A stored index is only meaningful for the vector size it was built with, so vectors saved
        // under one size must not be loaded under another; doing so would treat stale indexes as valid.
        final Path path = dir.resolve("vectors.json");
        final String context = "c";

        final FileBasedVectorService original = new FileBasedVectorService(path, 64, ALGORITHM);
        original.hashAndInsert(context, single(5, 64), span(), 64);
        original.save();

        // Reload with a different vector size: the persisted vectors must be discarded (cold start).
        final FileBasedVectorService reloaded = new FileBasedVectorService(path, 128, ALGORITHM);
        Assertions.assertTrue(reloaded.getVectorRepresentation(context, FilterType.SSN).isEmpty(),
                "vectors built with a different vector size should be discarded");
    }

    @Test
    public void vectorsBuiltWithADifferentHashAlgorithmAreDiscarded(@TempDir final Path dir) throws Exception {

        // The hash algorithm decides which index a token maps to, so vectors saved under one algorithm
        // are meaningless under another and must be discarded rather than silently reused.
        final Path path = dir.resolve("vectors.json");
        final String context = "c";

        final FileBasedVectorService original = new FileBasedVectorService(path, VECTOR_SIZE, "murmur3");
        original.hashAndInsert(context, single(5, VECTOR_SIZE), span(), VECTOR_SIZE);
        original.save();

        final FileBasedVectorService reloaded = new FileBasedVectorService(path, VECTOR_SIZE, "hashCode");
        Assertions.assertTrue(reloaded.getVectorRepresentation(context, FilterType.SSN).isEmpty(),
                "vectors built with a different hash algorithm should be discarded");
    }

    @Test
    public void equivalentHashAlgorithmNamesAreNotTreatedAsAMismatch(@TempDir final Path dir) throws Exception {

        // Anything that is not "murmur3" means String.hashCode(), so two such names are the same
        // algorithm and the persisted vectors must still load.
        final Path path = dir.resolve("vectors.json");
        final String context = "c";

        final FileBasedVectorService original = new FileBasedVectorService(path, VECTOR_SIZE, "hashCode");
        original.hashAndInsert(context, single(5, VECTOR_SIZE), span(), VECTOR_SIZE);
        original.save();

        final FileBasedVectorService reloaded = new FileBasedVectorService(path, VECTOR_SIZE, "java");
        Assertions.assertFalse(reloaded.getVectorRepresentation(context, FilterType.SSN).isEmpty(),
                "two non-murmur3 algorithm names mean the same thing and should still load");
    }

    private static double[] single(final int index, final int size) {
        final double[] hashes = new double[size];
        hashes[index] = 1;
        return hashes;
    }

    private static Span span() {
        return Span.make(0, 4, FilterType.SSN, "c", 0.0, "x", "x", "", false, true, new String[]{"x"}, 0);
    }

}
