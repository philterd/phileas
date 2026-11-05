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
package ai.philterd.phileas.services.documentprocessors;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorBasedSpanDisambiguationService;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import com.google.gson.Gson;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.NoOpSpanDisambiguationService;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationService;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationServiceFactory;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

class SpanDisambiguationPolicyTest {

    /** Records whether the pipeline asked for disambiguation. */
    private static class RecordingSpanDisambiguationService implements SpanDisambiguationService {

        private boolean called = false;

        @Override
        public void hashAndInsert(VectorService vectorService, String context, Span span) {
            // Nothing to record: only the disambiguate call matters here.
        }

        @Override
        public FilterType disambiguate(VectorService vectorService, String context, List<FilterType> filterTypes, Span ambiguousSpan) {
            return filterTypes.get(0);
        }

        @Override
        public List<Span> disambiguate(VectorService vectorService, String context, List<Span> spans) {
            called = true;
            return spans;
        }

    }

    private boolean disambiguationRan(final Policy policy) throws Exception {

        final RecordingSpanDisambiguationService service = new RecordingSpanDisambiguationService();

        new UnstructuredDocumentProcessor(service, false).process(new DefaultContextService(),
                new InMemoryVectorService(), policy, List.of(), List.of(), "context", 0, "the ssn is 123-45-6789");

        return service.called;

    }

    /** Two spans at the same place with different types: the case disambiguation exists to settle. */
    private static Filter ambiguousFilter() {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(4)
                .build();

        return new Filter(FilterType.SSN, filterConfiguration) {
            @Override
            public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) {
                final String[] window = {"phone", "number", "called", "is"};
                return new Filtered(context, piece, List.of(
                        Span.make(11, 22, FilterType.SSN, context, 0.9, "123-45-6789", "REDACTED-ssn", "", false, true, window, 0),
                        Span.make(11, 22, FilterType.PHONE_NUMBER, context, 0.9, "123-45-6789", "REDACTED-phone", "", false, true, window, 0)));
            }
        };

    }

    private VectorBasedSpanDisambiguationService trainedOnPhoneNumbers(final VectorService vectorService) {

        final Properties properties = new Properties();
        properties.setProperty("span.disambiguation.enabled", "true");
        properties.setProperty("span.disambiguation.ignore.stopwords", "false");
        properties.setProperty("span.disambiguation.vector.size", "32");

        final VectorBasedSpanDisambiguationService service =
                new VectorBasedSpanDisambiguationService(new PhileasConfiguration(properties));

        service.hashAndInsert(vectorService, "context", Span.make(0, 11, FilterType.SSN, "context", 0.0,
                "123-45-6789", "x", "", false, true, new String[]{"ssn", "was", "he", "id"}, 0));
        service.hashAndInsert(vectorService, "context", Span.make(0, 11, FilterType.PHONE_NUMBER, "context", 0.0,
                "123-45-6789", "x", "", false, true, new String[]{"phone", "number", "called", "is"}, 0));

        return service;

    }

    private List<Span> spansFor(final Policy policy) throws Exception {

        final VectorService vectorService = new InMemoryVectorService();

        return new UnstructuredDocumentProcessor(trainedOnPhoneNumbers(vectorService), false)
                .process(new DefaultContextService(), vectorService, policy, List.of(ambiguousFilter()),
                        List.of(), "context", 0, "the ssn is 123-45-6789")
                .getExplanation().appliedSpans();

    }

    @Test
    void realDisambiguationPicksTheLearnedTypeWhenThePolicyAllowsIt() throws Exception {

        final List<Span> spans = spansFor(new Policy());

        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals(FilterType.PHONE_NUMBER, spans.get(0).getFilterType());

    }

    @Test
    void skippingDisambiguationLeavesTheTypeToTheSpanRanking() throws Exception {

        final Policy policy = new Policy();
        policy.getConfig().getAnalysis().setSpanDisambiguation(false);

        final List<Span> spans = spansFor(policy);

        // Still one span, but chosen by ranking rather than by context.
        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals(FilterType.SSN, spans.get(0).getFilterType());

    }

    @Test
    void aPolicyThatDoesNotSetTheFlagRunsDisambiguation() throws Exception {
        Assertions.assertTrue(disambiguationRan(new Policy()));
    }

    @Test
    void aPolicyWithANullAnalysisSectionStillFilters() throws Exception {

        // Gson leaves the section null, and the guard has to tolerate it.
        final Policy policy = new Gson().fromJson("{ \"config\": { \"analysis\": null } }", Policy.class);

        Assertions.assertNull(policy.getConfig().getAnalysis());
        Assertions.assertTrue(disambiguationRan(policy));

    }

    @Test
    void aPolicySettingTheFlagTrueRunsDisambiguation() throws Exception {

        final Policy policy = new Policy();
        policy.getConfig().getAnalysis().setSpanDisambiguation(true);

        Assertions.assertTrue(disambiguationRan(policy));

    }

    @Test
    void aPolicySettingTheFlagFalseSkipsDisambiguation() throws Exception {

        final Policy policy = new Policy();
        policy.getConfig().getAnalysis().setSpanDisambiguation(false);

        Assertions.assertFalse(disambiguationRan(policy));

    }

    @Test
    void aPolicyCannotTurnDisambiguationOnWhenItIsOffGlobally() {

        // Off globally injects the no-op, whatever the policy asks.
        final Properties properties = new Properties();
        properties.setProperty("span.disambiguation.enabled", "false");

        final SpanDisambiguationService service = SpanDisambiguationServiceFactory
                .getSpanDisambiguationService(new PhileasConfiguration(properties));

        Assertions.assertInstanceOf(NoOpSpanDisambiguationService.class, service);

    }

    @Test
    void theGlobalSettingSelectsTheVectorImplementationWhenOn() {

        final Properties properties = new Properties();
        properties.setProperty("span.disambiguation.enabled", "true");

        final SpanDisambiguationService service = SpanDisambiguationServiceFactory
                .getSpanDisambiguationService(new PhileasConfiguration(properties));

        Assertions.assertFalse(service instanceof NoOpSpanDisambiguationService);

    }

    @Test
    void skippingDisambiguationLeavesTheSpansUnchanged() throws Exception {

        // The pipeline must still redact; only the disambiguation step is skipped.
        final Policy policy = new Policy();
        policy.getConfig().getAnalysis().setSpanDisambiguation(false);

        final RecordingSpanDisambiguationService service = new RecordingSpanDisambiguationService();

        final var result = new UnstructuredDocumentProcessor(service, false).process(new DefaultContextService(),
                new InMemoryVectorService(), policy, List.of(), List.of(), "context", 0, "no filters here");

        Assertions.assertEquals("no filters here", result.getFilteredText());
        Assertions.assertFalse(service.called);

    }

}
