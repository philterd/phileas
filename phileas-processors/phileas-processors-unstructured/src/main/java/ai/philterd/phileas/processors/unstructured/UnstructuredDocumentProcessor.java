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
package ai.philterd.phileas.processors.unstructured;

import ai.philterd.phileas.model.filter.Filter;
import ai.philterd.phileas.model.objects.Explanation;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.responses.FilterResponse;
import ai.philterd.phileas.model.services.DocumentProcessor;
import ai.philterd.phileas.model.services.MetricsService;
import ai.philterd.phileas.model.services.PostFilter;
import ai.philterd.phileas.model.services.SpanDisambiguationService;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

/**
 * Processes and filters unstructured text documents.
 */
public class UnstructuredDocumentProcessor implements DocumentProcessor {

    private final MetricsService metricsService;
    private final SpanDisambiguationService spanDisambiguationService;

    public UnstructuredDocumentProcessor(final MetricsService metricsService,
                                         final SpanDisambiguationService spanDisambiguationService) {

        this.metricsService = metricsService;
        this.spanDisambiguationService = spanDisambiguationService;

    }

    @Override
    public FilterResponse process(final Policy policy, final List<Filter> filters, final List<PostFilter> postFilters,
                                  final String context, final String documentId, final int piece, final String input,
                                  final Map<String, String> attributes) throws Exception {

        // The list that will contain the spans containing PHI/PII.
        List<Span> identifiedSpans = new LinkedList<>();

        // Apply each filter.
        for(final Filter filter : filters) {

            final long startTimeMs = System.currentTimeMillis();
            final FilterResult filterResult = filter.filter(policy, context, documentId, piece, input, attributes);
            final long elapsedTimeMs = System.currentTimeMillis() - startTimeMs;

            metricsService.logFilterTime(filter.getFilterType(), elapsedTimeMs);

            identifiedSpans.addAll(filterResult.getSpans());

        }

        // Perform span disambiguation.
        if(spanDisambiguationService.isEnabled()) {
            identifiedSpans = spanDisambiguationService.disambiguate(context, identifiedSpans);
        }

        // Drop overlapping spans.
        identifiedSpans = Span.dropOverlappingSpans(identifiedSpans);

        // Sort the spans based on the confidence.
        identifiedSpans.sort(Comparator.comparing(Span::getConfidence));

        // Remove equal spans having lower priorities.


        // Perform post-filtering on the spans.
        for(final PostFilter postFilter : postFilters) {
            identifiedSpans = postFilter.filter(input, identifiedSpans);
        }

        // PHL-185: Remove non-adjacent firstname/surname spans.
        /*// A first name filter must be adjacent to a surname filter.
        final List<Span> dontRemove = new LinkedList<>();
        for(final Span span1 : spans) {

            for(final Span span2 : spans) {

                if(span1.getFilterType() == FilterType.FIRST_NAME && span2.getFilterType() == FilterType.SURNAME) {

                    // Are they adjacent?
                    if(Span.areSpansAdjacent(span1, span2, input)) {

                        // Yes, don't remove them.
                        dontRemove.add(span1);
                        dontRemove.add(span2);

                    }

                }

            }

        }

        // Remove all first name / surname spans.
        final List<Span> doRemove = new LinkedList<>();
        for(final Span span : spans) {
            if(span.getFilterType() == FilterType.FIRST_NAME || span.getFilterType() == FilterType.SURNAME) {
                doRemove.add(span);
            }
        }
        spans.removeAll(doRemove);

        // Add back adjacent spans.
        spans.addAll(dontRemove);*/

        // The spans that will be persisted. Has to be a deep copy because the shift
        // below will change the indexes. Doing this to save the original locations of the spans.
        List<Span> appliedSpans = identifiedSpans.stream().filter(Span::isApplied)
                .filter(Predicate.not(Span::isIgnored)).map(Span::copy).collect(toList());

        // TODO: Set a flag on each "span" not in appliedSpans indicating it was not used.

        // Log a metric for each filter type.
        appliedSpans.forEach(k -> metricsService.incrementFilterType(k.getFilterType()));

        // Define the explanation.
        final Explanation explanation = new Explanation(appliedSpans, identifiedSpans);

        // Used to manipulate the text.
        final StringBuilder sb = new StringBuilder(input);

        // Initialize this to the input length, but it may grow in length if redactions/replacements
        // are longer than the original spans.
        int stringLength = input.length();

        // Do the actual replacement of spans in the text.
        // Go character by character through the input.
        for(int i = 0; i < stringLength; i++) {

            // Is index i the start of a span?
            final Span span = Span.doesIndexStartSpan(i, appliedSpans);

            if(span != null) {

                // Get the replacement. This might be the token itself or an anonymized version.
                final String replacement = span.getReplacement();

                final int spanLength = span.getCharacterEnd() - span.getCharacterStart();
                final int replacementLength = replacement.length();

                if(spanLength != replacementLength) {

                    // We need to adjust the characterStart and characterEnd for the remaining spans.
                    // A negative value means shift left.
                    // A positive value means shift right.
                    final int shift = (spanLength - replacementLength) * -1;

                    // Shift the remaining spans by the shift value.
                    appliedSpans = Span.shiftSpans(shift, span, appliedSpans);

                    // Update the length of the string.
                    stringLength += shift;

                }

                // We can now do the replacement.
                sb.replace(span.getCharacterStart(), span.getCharacterEnd(), replacement);

                // Jump ahead outside of this span.
                i = span.getCharacterEnd();

            }

        }

        metricsService.incrementProcessed();

        return new FilterResponse(sb.toString(), context, documentId, piece, explanation, attributes);

    }

}
