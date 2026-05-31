/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.disambiguation;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.documentprocessors.UnstructuredDocumentProcessor;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorBasedSpanDisambiguationService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class VectorBasedSpanDisambiguationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(VectorBasedSpanDisambiguationServiceTest.class);

    @Test
    public void disambiguateLocal1() throws IOException {

        final VectorService vectorService = new InMemoryVectorService();

        final Properties properties = new Properties();

        properties.setProperty("span.disambiguation.enabled", "true");
        properties.setProperty("span.disambiguation.ignore.stopwords", "false");
        properties.setProperty("span.disambiguation.vector.size", "32");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final String context = "c";

        final VectorBasedSpanDisambiguationService vectorBasedSpanDisambiguationService = new VectorBasedSpanDisambiguationService(phileasConfiguration, vectorService);

        final Span span1 = Span.make(0, 4, FilterType.SSN, context, 0.00, "123-45-6789", "000-00-0000", "", false, true, new String[]{"ssn", "was", "he", "id"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span1);

        final Span span = Span.make(0, 4, FilterType.SSN, context, 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"ssn", "asdf", "he", "was"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span);

        final Span span2 = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"phone", "number", "she", "had"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span2);

        final List<FilterType> filterTypes = Arrays.asList(span1.getFilterType(), span2.getFilterType());

        final Span ambiguousSpan = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"phone", "number", "called", "is"}, 0);
        final FilterType filterType = vectorBasedSpanDisambiguationService.disambiguate(context, filterTypes, ambiguousSpan);

        Assertions.assertEquals(FilterType.PHONE_NUMBER, filterType);

    }

    @Test
    public void disambiguateLocal2() throws IOException {

        final VectorService vectorService = new InMemoryVectorService();

        final Properties properties = new Properties();

        properties.setProperty("span.disambiguation.enabled", "true");
        properties.setProperty("span.disambiguation.ignore.stopwords", "false");
        properties.setProperty("span.disambiguation.vector.size", "32");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final String context = "c";

        final VectorBasedSpanDisambiguationService vectorBasedSpanDisambiguationService = new VectorBasedSpanDisambiguationService(phileasConfiguration, vectorService);

        final Span span1 = Span.make(0, 4, FilterType.SSN, context, 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"ssn", "was", "he", "id"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span1);

        final Span span = Span.make(0, 4, FilterType.SSN, context, 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"ssn", "asdf", "he", "was"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span);

        final Span span2 = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"phone", "number", "she", "had"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span2);

        final Span ambiguousSpan = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", 0.00, "123-45-6789", "000-00-0000", "",  false, true, new String[]{"phone", "number", "called", "is"}, 0);

        final List<Span> spans = Arrays.asList(span, span1, span2, ambiguousSpan);

        final List<Span> disambiguatedSpans = vectorBasedSpanDisambiguationService.disambiguate(context, spans);

        showSpans(disambiguatedSpans);

        // Each competing span is resolved to the filter type its own surrounding context supports:
        // the spans with SSN-like windows resolve to SSN, and the spans with phone-like windows
        // resolve to PHONE_NUMBER. (The earlier implementation incorrectly collapsed every span to
        // a single type regardless of its context.) After resolution the duplicate per-type spans
        // at the same location dedupe, leaving one SSN and one PHONE_NUMBER span.
        final Set<FilterType> resolvedTypes = disambiguatedSpans.stream()
                .map(Span::getFilterType).collect(java.util.stream.Collectors.toSet());

        Assertions.assertEquals(2, disambiguatedSpans.size());
        Assertions.assertTrue(resolvedTypes.contains(FilterType.SSN), "an SSN-context span should resolve to SSN");
        Assertions.assertTrue(resolvedTypes.contains(FilterType.PHONE_NUMBER), "a phone-context span should resolve to PHONE_NUMBER");

    }

    private VectorBasedSpanDisambiguationService service(final VectorService vectorService) {
        final Properties properties = new Properties();
        properties.setProperty("span.disambiguation.enabled", "true");
        properties.setProperty("span.disambiguation.ignore.stopwords", "false");
        properties.setProperty("span.disambiguation.vector.size", "32");
        return new VectorBasedSpanDisambiguationService(new PhileasConfiguration(properties), vectorService);
    }

    /**
     * Builds a service from explicit property overrides on top of the test defaults
     * (enabled, vector size 32) so individual tests can flip stop word handling and the
     * hash algorithm.
     */
    private VectorBasedSpanDisambiguationService service(final VectorService vectorService, final Map<String, String> overrides) {
        final Properties properties = new Properties();
        properties.setProperty("span.disambiguation.enabled", "true");
        properties.setProperty("span.disambiguation.vector.size", "32");
        overrides.forEach(properties::setProperty);
        return new VectorBasedSpanDisambiguationService(new PhileasConfiguration(properties), vectorService);
    }

    @Test
    public void stopWordsAreParsedAndRemovedFromTheVector() {

        // A window made up entirely of (multi-word, comma-separated, mixed-case) stop words must
        // contribute nothing to the learned vector when stop word handling is on. This guards the
        // comma-splitting parse: the previous implementation split on "" and produced per-character
        // "words", so real words were never recognized as stop words.
        final InMemoryVectorService withStopWords = new InMemoryVectorService();
        final VectorBasedSpanDisambiguationService ignoring = service(withStopWords, Map.of(
                "span.disambiguation.ignore.stopwords", "true",
                "span.disambiguation.stopwords", "alpha, beta"));

        // Mixed case proves the stop word match is case-insensitive.
        final Span span = Span.make(0, 4, FilterType.SSN, "c", 0.0, "x", "x", "",
                false, true, new String[]{"Alpha", "BETA"}, 0);
        ignoring.hashAndInsert("c", span);

        Assertions.assertTrue(withStopWords.getVectorRepresentation("c", FilterType.SSN).isEmpty(),
                "a window of only stop words should not contribute to the vector");

        // With stop word handling off, the very same window does contribute.
        final InMemoryVectorService noStopWords = new InMemoryVectorService();
        final VectorBasedSpanDisambiguationService keeping = service(noStopWords, Map.of(
                "span.disambiguation.ignore.stopwords", "false",
                "span.disambiguation.stopwords", "alpha, beta"));
        keeping.hashAndInsert("c", Span.make(0, 4, FilterType.SSN, "c", 0.0, "x", "x", "",
                false, true, new String[]{"Alpha", "BETA"}, 0));

        Assertions.assertFalse(noStopWords.getVectorRepresentation("c", FilterType.SSN).isEmpty(),
                "the same window should contribute when stop words are not ignored");
    }

    @Test
    public void hashAlgorithmSelectionUsesTheConfiguredAlgorithm() {

        // The configured/default algorithm is "murmur3". A previous typo ("murmu3") meant the check
        // never matched, so hashing silently fell back to String.hashCode() for every deployment.
        final VectorService vectorService = new InMemoryVectorService();
        final VectorBasedSpanDisambiguationService defaultAlgo = service(vectorService, Map.of());
        final VectorBasedSpanDisambiguationService murmur3 = service(vectorService,
                Map.of("span.disambiguation.hash.algorithm", "murmur3"));
        final VectorBasedSpanDisambiguationService hashCode = service(vectorService,
                Map.of("span.disambiguation.hash.algorithm", "hashCode"));

        final String[] tokens = {"phone", "number", "ssn", "called", "office", "social", "security"};

        boolean murmurDiffersFromHashCode = false;
        for(final String token : tokens) {
            // The default must hash identically to an explicit murmur3 service: this is what fails
            // if the algorithm name check regresses and the default silently becomes hashCode.
            Assertions.assertEquals(murmur3.hashToken(token), defaultAlgo.hashToken(token),
                    "the default algorithm should be murmur3");
            if(murmur3.hashToken(token) != hashCode.hashToken(token)) {
                murmurDiffersFromHashCode = true;
            }
        }

        // Sanity: the two branches really do compute different hashes, so the assertions above are
        // meaningful rather than coincidentally equal.
        Assertions.assertTrue(murmurDiffersFromHashCode,
                "murmur3 and hashCode should produce different hashes for at least one token");
    }

    @Test
    public void hashingIsCaseInsensitive() {

        // Train PHONE_NUMBER on lower-cased phone context, then disambiguate an ambiguous span whose
        // window is the same words but capitalized. Because token hashing is case-insensitive, the
        // capitalized window still matches the learned vector and PHONE_NUMBER wins over the
        // first-listed (untrained) SSN candidate. Before the fix the capitalized tokens hashed to
        // different indexes, the overlap was zero, and the cold-start fallback returned SSN.
        //
        // A large vector size is used so a case mismatch genuinely produces no overlap; with a small
        // vector, hash collisions would make the capitalized tokens overlap by chance and the test
        // would pass even with the bug present.
        final VectorBasedSpanDisambiguationService service = service(new InMemoryVectorService(),
                Map.of("span.disambiguation.vector.size", "4096", "span.disambiguation.ignore.stopwords", "false"));
        final String context = "c";

        service.hashAndInsert(context, Span.make(0, 4, FilterType.PHONE_NUMBER, context, 0.0, "555-1212", "x", "",
                false, true, new String[]{"phone", "number", "call"}, 0));

        final List<FilterType> candidates = Arrays.asList(FilterType.SSN, FilterType.PHONE_NUMBER);
        final Span ambiguousSpan = Span.make(0, 4, FilterType.SSN, context, 0.0, "123-4567", "x", "",
                false, true, new String[]{"Phone", "Number", "Call"}, 0);

        Assertions.assertEquals(FilterType.PHONE_NUMBER, service.disambiguate(context, candidates, ambiguousSpan),
                "a capitalized window should match the lower-cased trained vector");
    }

    @Test
    public void competingSpansSharingAWindowResolveToTheTrainedTypeAndDedupe() {

        // The realistic pipeline case: two filters flag the exact same text at the same location,
        // so both competing spans carry the SAME context window. After training PHONE_NUMBER on a
        // phone-like context, both competing spans must resolve to PHONE_NUMBER and, being identical
        // once their filter type matches, collapse to a single span.
        final VectorBasedSpanDisambiguationService service = service(new InMemoryVectorService());
        final String context = "c";

        final String[] window = {"phone", "number", "call", "office"};

        // Train PHONE_NUMBER on the phone-like context.
        service.hashAndInsert(context, Span.make(0, 4, FilterType.PHONE_NUMBER, context, 0.0, "555-1212", "x", "",
                false, true, window, 0));

        // Two competing spans at the same location with the SAME window (as the real pipeline emits)
        // differing only by filter type. Everything else is identical so they dedupe once resolved.
        final Span asSsn = Span.make(0, 4, FilterType.SSN, context, 0.5, "123-45-6789", "x", "",
                false, true, window, 0);
        final Span asPhone = Span.make(0, 4, FilterType.PHONE_NUMBER, context, 0.5, "123-45-6789", "x", "",
                false, true, window, 0);

        final List<Span> resolved = service.disambiguate(context, Arrays.asList(asSsn, asPhone));

        Assertions.assertEquals(1, resolved.size(), "identically-located spans should dedupe after resolution");
        Assertions.assertEquals(FilterType.PHONE_NUMBER, resolved.get(0).getFilterType(),
                "the shared phone-like context should resolve to PHONE_NUMBER");
    }

    @Test
    public void disambiguationRunsThroughTheProcessorOnlyWhenEnabled() throws Exception {

        // The processor must call disambiguate() exactly when the service reports enabled, and skip
        // it otherwise. A recording stub makes the gate observable without depending on downstream
        // overlap-dropping behavior.
        final String context = "c";
        final String input = "1234 was the number";

        final Span asSsn = Span.make(0, 4, FilterType.SSN, context, 0.5, "1234", "x", "",
                false, true, new String[]{"phone", "number"}, 0);
        final Span asPhone = Span.make(0, 4, FilterType.PHONE_NUMBER, context, 0.5, "1234", "x", "",
                false, true, new String[]{"phone", "number"}, 0);
        final Filter filter = fakeFilter(Arrays.asList(asSsn, asPhone));

        // Enabled: disambiguate() is invoked.
        final RecordingSpanDisambiguationService enabled = new RecordingSpanDisambiguationService(true);
        new UnstructuredDocumentProcessor(enabled, false)
                .process(new Policy(), List.of(filter), Collections.emptyList(), context, 0, input);
        Assertions.assertTrue(enabled.disambiguateCalled, "disambiguate() should be called when enabled");

        // Disabled: disambiguate() is skipped.
        final RecordingSpanDisambiguationService disabled = new RecordingSpanDisambiguationService(false);
        new UnstructuredDocumentProcessor(disabled, false)
                .process(new Policy(), List.of(filter), Collections.emptyList(), context, 0, input);
        Assertions.assertFalse(disabled.disambiguateCalled, "disambiguate() should be skipped when disabled");
    }

    /**
     * A minimal {@link Filter} that returns a fixed set of spans, used to drive the processor without
     * standing up real identifier filters.
     */
    private Filter fakeFilter(final List<Span> spans) {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder().build();
        return new Filter(FilterType.SSN, filterConfiguration) {
            @Override
            public Filtered filter(final Policy policy, final String context, final int piece, final String input) {
                return new Filtered(context, piece, spans);
            }
        };
    }

    /**
     * A {@link SpanDisambiguationService} stub that records whether disambiguate() was invoked and
     * otherwise returns the spans unchanged.
     */
    private static final class RecordingSpanDisambiguationService implements SpanDisambiguationService {

        private final boolean enabled;
        private boolean disambiguateCalled = false;

        private RecordingSpanDisambiguationService(final boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void hashAndInsert(final String context, final Span span) {
            // No-op for the stub.
        }

        @Override
        public FilterType disambiguate(final String context, final List<FilterType> filterTypes, final Span ambiguousSpan) {
            disambiguateCalled = true;
            return filterTypes.get(0);
        }

        @Override
        public List<Span> disambiguate(final String context, final List<Span> spans) {
            disambiguateCalled = true;
            return spans;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }
    }

    @Test
    public void coldStartIsDeterministicAndReturnsACandidate() {

        // No vectors have been stored. The decision must be deterministic (first candidate) rather
        // than undefined (the old code produced NaN cosine similarities).
        final VectorBasedSpanDisambiguationService service = service(new InMemoryVectorService());

        final Span ambiguousSpan = Span.make(0, 4, FilterType.SSN, "c", 0.0, "123-45-6789", "x", "",
                false, true, new String[]{"some", "unseen", "words"}, 0);
        final List<FilterType> candidates = Arrays.asList(FilterType.SSN, FilterType.PHONE_NUMBER);

        final FilterType first = service.disambiguate("c", candidates, ambiguousSpan);
        final FilterType second = service.disambiguate("c", candidates, ambiguousSpan);

        Assertions.assertEquals(FilterType.SSN, first, "cold start should fall back to the first candidate");
        Assertions.assertEquals(first, second, "cold start must be deterministic");
    }

    @Test
    public void trainingImprovesTheDecision() {

        // After learning that phone-related context goes with PHONE_NUMBER, an ambiguous span in a
        // phone-like context resolves to PHONE_NUMBER rather than the cold-start first candidate.
        final VectorBasedSpanDisambiguationService service = service(new InMemoryVectorService());
        final String context = "c";

        // Train: unambiguous PHONE_NUMBER spans seen in phone-like contexts.
        service.hashAndInsert(context, Span.make(0, 4, FilterType.PHONE_NUMBER, context, 0.0, "555-1212", "x", "",
                false, true, new String[]{"phone", "number", "call", "reached"}, 0));
        service.hashAndInsert(context, Span.make(0, 4, FilterType.PHONE_NUMBER, context, 0.0, "555-3434", "x", "",
                false, true, new String[]{"phone", "number", "dial", "office"}, 0));

        // SSN candidate is listed FIRST so a win for PHONE_NUMBER cannot be a cold-start artifact.
        final List<FilterType> candidates = Arrays.asList(FilterType.SSN, FilterType.PHONE_NUMBER);

        final Span ambiguousSpan = Span.make(0, 4, FilterType.SSN, context, 0.0, "123-4567", "x", "",
                false, true, new String[]{"phone", "number", "call", "office"}, 0);

        Assertions.assertEquals(FilterType.PHONE_NUMBER, service.disambiguate(context, candidates, ambiguousSpan),
                "learned phone context should win over the first (SSN) candidate");
    }

    @Test
    public void unambiguousSpansAreUsedAsTrainingData() {

        // The list-level disambiguate() should train on unambiguous spans so the store is populated
        // even without explicit hashAndInsert calls.
        final InMemoryVectorService vectorService = new InMemoryVectorService();
        final VectorBasedSpanDisambiguationService service = service(vectorService);
        final String context = "c";

        // A single unambiguous PHONE_NUMBER span (no competing span at the same location).
        final Span phone = Span.make(0, 4, FilterType.PHONE_NUMBER, context, 0.0, "555-1212", "x", "",
                false, true, new String[]{"phone", "number", "call"}, 0);

        service.disambiguate(context, List.of(phone));

        Assertions.assertFalse(vectorService.getVectorRepresentation(context, FilterType.PHONE_NUMBER).isEmpty(),
                "an unambiguous span should have been recorded as training data");
    }

    public void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}
