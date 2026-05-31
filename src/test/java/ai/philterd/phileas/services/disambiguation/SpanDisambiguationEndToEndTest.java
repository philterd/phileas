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
package ai.philterd.phileas.services.disambiguation;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.filters.AbstractFilterTest;
import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorBasedSpanDisambiguationService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.documentprocessors.UnstructuredDocumentProcessor;
import ai.philterd.phileas.services.filters.regex.IdentifierFilter;
import ai.philterd.phileas.services.filters.regex.SsnFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.IdentifierFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * End-to-end span disambiguation test that runs real text through the real filter pipeline
 * ({@link UnstructuredDocumentProcessor} with real {@link SsnFilter} and {@link IdentifierFilter}),
 * using an in-memory vector store. It verifies the behavior the user documentation describes: when
 * the same value is detected as two different PII types at the same location, the surrounding
 * context decides the winner, and the decision improves as the context is trained.
 *
 * <p>Note: a bare nine-digit number is used because that is a value that two filters genuinely claim
 * at identical character positions. (A formatted SSN like {@code 123-45-6789} and a formatted phone
 * number like {@code 123-456-7890} have structurally different patterns and never produce competing
 * spans, so they cannot exercise disambiguation; this was confirmed against the live filters.)
 */
public class SpanDisambiguationEndToEndTest extends AbstractFilterTest {

    /** A bare nine-digit value matched by both the SSN filter and the custom identifier filter. */
    private static final String NUMBER = "123456789";

    /** Custom identifier regex that matches the same nine digits the SSN filter matches. */
    private static final String IDENTIFIER_REGEX = "\\b\\d{9}\\b";

    private VectorBasedSpanDisambiguationService disambiguationService(final VectorService vectorService) {
        final Properties properties = new Properties();
        properties.setProperty("span.disambiguation.enabled", "true");
        properties.setProperty("span.disambiguation.ignore.stopwords", "true");
        properties.setProperty("span.disambiguation.vector.size", "64");
        return new VectorBasedSpanDisambiguationService(new PhileasConfiguration(properties), vectorService);
    }

    private FilterConfiguration filterConfiguration(final AbstractFilterStrategy strategy) {
        return new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(strategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();
    }

    /**
     * Runs the given input through the full pipeline and returns the single span left at the
     * location of the nine-digit value after disambiguation and overlap resolution.
     */
    private Span resolveNumberSpan(final VectorBasedSpanDisambiguationService disambiguation,
                                   final String input) throws Exception {

        final Filter ssnFilter = new SsnFilter(filterConfiguration(new SsnFilterStrategy()));
        final Filter identifierFilter = new IdentifierFilter(
                filterConfiguration(new IdentifierFilterStrategy()), "id", IDENTIFIER_REGEX, true, 0);

        final UnstructuredDocumentProcessor processor = new UnstructuredDocumentProcessor(disambiguation, false);

        final TextFilterResult result = processor.process(getPolicy(),
                List.of(ssnFilter, identifierFilter), Collections.emptyList(), "ctx", PIECE, input);

        // After disambiguation + overlap resolution there should be exactly one span at the number.
        final List<Span> spans = result.getExplanation().identifiedSpans().stream()
                .filter(s -> NUMBER.equals(s.getText())).toList();

        Assertions.assertEquals(1, spans.size(),
                "exactly one span should remain at the ambiguous value after disambiguation");

        return spans.get(0);

    }

    @Test
    public void competingSpansAreProducedAtTheSameLocation() throws Exception {

        // Sanity check the premise of the whole feature: the two filters really do claim the same
        // characters with different types, which is what makes disambiguation necessary.
        final Filter ssnFilter = new SsnFilter(filterConfiguration(new SsnFilterStrategy()));
        final Filter identifierFilter = new IdentifierFilter(
                filterConfiguration(new IdentifierFilterStrategy()), "id", IDENTIFIER_REGEX, true, 0);

        final String input = "The number " + NUMBER + " is here.";

        final List<Span> ssnSpans = ssnFilter.filter(getPolicy(), "ctx", PIECE, input).getSpans();
        final List<Span> idSpans = identifierFilter.filter(getPolicy(), "ctx", PIECE, input).getSpans();

        Assertions.assertEquals(1, ssnSpans.size());
        Assertions.assertEquals(1, idSpans.size());
        Assertions.assertEquals(ssnSpans.get(0).getCharacterStart(), idSpans.get(0).getCharacterStart());
        Assertions.assertEquals(ssnSpans.get(0).getCharacterEnd(), idSpans.get(0).getCharacterEnd());
        Assertions.assertNotEquals(ssnSpans.get(0).getFilterType(), idSpans.get(0).getFilterType());
        // Windows must be populated or disambiguation has nothing to work with.
        Assertions.assertTrue(ssnSpans.get(0).getWindow().length > 0, "the SSN span's context window must be populated");
    }

    @Test
    public void contextTrainedForSsnResolvesAmbiguousValueToSsn() throws Exception {

        final VectorBasedSpanDisambiguationService disambiguation = disambiguationService(new InMemoryVectorService());

        // Train: process documents where the nine-digit value appears unambiguously in SSN-like
        // context. (Only the SSN filter is configured to match in these training documents, so the
        // span is unambiguous and is recorded as SSN training data by the pipeline.)
        trainSsn(disambiguation, "The patient ssn social security number is " + NUMBER + " on record.");
        trainSsn(disambiguation, "Their social security ssn was listed as " + NUMBER + " here.");

        // Now resolve an ambiguous occurrence sitting in clearly SSN-like context.
        final Span resolved = resolveNumberSpan(disambiguation,
                "The ssn social security number " + NUMBER + " was confirmed.");

        Assertions.assertEquals(FilterType.SSN, resolved.getFilterType(),
                "an ambiguous value in SSN-like context should resolve to SSN");

    }

    @Test
    public void contextTrainedForIdentifierResolvesAmbiguousValueToIdentifier() throws Exception {

        final VectorBasedSpanDisambiguationService disambiguation = disambiguationService(new InMemoryVectorService());

        // Train: the nine-digit value appears unambiguously in employee-identifier context.
        trainIdentifier(disambiguation, "The employee badge identifier " + NUMBER + " was assigned.");
        trainIdentifier(disambiguation, "Employee badge identifier number " + NUMBER + " is active.");

        // Resolve an ambiguous occurrence sitting in employee-identifier context.
        final Span resolved = resolveNumberSpan(disambiguation,
                "The employee badge identifier " + NUMBER + " is shown.");

        Assertions.assertEquals(FilterType.IDENTIFIER, resolved.getFilterType(),
                "an ambiguous value in identifier-like context should resolve to IDENTIFIER");

    }

    @Test
    public void identicalInputResolvesIdenticallyAcrossRuns() throws Exception {

        // Determinism: the same trained store and the same input must yield the same decision.
        final VectorBasedSpanDisambiguationService disambiguation = disambiguationService(new InMemoryVectorService());
        trainIdentifier(disambiguation, "The employee badge identifier " + NUMBER + " was assigned.");

        final String input = "The employee badge identifier " + NUMBER + " is shown.";
        final FilterType first = resolveNumberSpan(disambiguation, input).getFilterType();
        final FilterType second = resolveNumberSpan(disambiguation, input).getFilterType();

        Assertions.assertEquals(first, second);

    }

    /** Trains the store with an SSN-only document so the value is recorded as SSN context. */
    private void trainSsn(final VectorBasedSpanDisambiguationService disambiguation, final String input) throws Exception {
        final Filter ssnFilter = new SsnFilter(filterConfiguration(new SsnFilterStrategy()));
        final UnstructuredDocumentProcessor processor = new UnstructuredDocumentProcessor(disambiguation, false);
        processor.process(getPolicy(), List.of(ssnFilter), Collections.emptyList(), "ctx", PIECE, input);
    }

    /** Trains the store with an identifier-only document so the value is recorded as IDENTIFIER context. */
    private void trainIdentifier(final VectorBasedSpanDisambiguationService disambiguation, final String input) throws Exception {
        final Filter identifierFilter = new IdentifierFilter(
                filterConfiguration(new IdentifierFilterStrategy()), "id", IDENTIFIER_REGEX, true, 0);
        final UnstructuredDocumentProcessor processor = new UnstructuredDocumentProcessor(disambiguation, false);
        processor.process(getPolicy(), List.of(identifierFilter), Collections.emptyList(), "ctx", PIECE, input);
    }

}
