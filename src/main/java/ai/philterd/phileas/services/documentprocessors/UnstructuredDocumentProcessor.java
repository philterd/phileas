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
package ai.philterd.phileas.services.documentprocessors;

import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.model.objects.Explanation;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.IncrementalRedaction;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.objects.FilterResponse;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationService;
import ai.philterd.phileas.services.tokens.TokenCounter;
import ai.philterd.phileas.services.tokens.WhitespaceTokenCounter;
import ai.philterd.phileas.policy.Policy;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
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

    private final SpanDisambiguationService spanDisambiguationService;
    private final boolean incrementalRedactionsEnabled;
    private final TokenCounter tokenCounter;

    public UnstructuredDocumentProcessor(final SpanDisambiguationService spanDisambiguationService,
                                         final boolean incrementalRedactionsEnabled) {

        this.spanDisambiguationService = spanDisambiguationService;
        this.incrementalRedactionsEnabled = incrementalRedactionsEnabled;
        this.tokenCounter = new WhitespaceTokenCounter();

    }

    @Override
    public FilterResponse process(final Policy policy, final List<Filter> filters, final List<PostFilter> postFilters,
                                  final String context, final int piece, final String input,
                                  final Map<String, String> attributes) throws Exception {

        // The list that will contain the spans containing PHI/PII.
        List<Span> identifiedSpans = new LinkedList<>();

        // Apply each filter.
        for(final Filter filter : filters) {

            final long startTimeMs = System.currentTimeMillis();
            final FilterResult filterResult = filter.filter(policy, context, piece, input, attributes);
            final long elapsedTimeMs = System.currentTimeMillis() - startTimeMs;

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

        // Perform post-filtering on the spans.
        for(final PostFilter postFilter : postFilters) {
            if(!postFilter.skipped()) {
                identifiedSpans = postFilter.filter(input, identifiedSpans);
            }
        }

        // The spans that will be persisted. Has to be a deep copy because the shift
        // below will change the indexes. Doing this to save the original locations of the spans.
        List<Span> appliedSpans = identifiedSpans.stream().filter(Span::isApplied)
                .filter(Predicate.not(Span::isIgnored)).map(Span::copy).collect(toList());

        // TODO: Set a flag on each "span" not in appliedSpans indicating it was not used.

        // Define the explanation.
        final Explanation explanation = new Explanation(appliedSpans, identifiedSpans);

        // Used to manipulate the text.
        final StringBuilder sb = new StringBuilder(input);

        // Initialize this to the input length, but it may grow in length if redactions/replacements
        // are longer than the original spans.
        int stringLength = input.length();

        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();
        final long tokens = tokenCounter.countTokens(input);

        // Do the actual replacement of spans in the text by going character by character through the input.
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

                if(incrementalRedactionsEnabled) {
                    // Hash the text at this point.
                    final String hash = DigestUtils.sha256Hex(sb.toString());
                    incrementalRedactions.add(new IncrementalRedaction(hash, span, sb.toString()));
                }

                // Jump ahead outside of this span.
                i = span.getCharacterEnd();

            }

        }

        return new FilterResponse(sb.toString(), context, piece, explanation, attributes, incrementalRedactions, tokens);

    }

}
