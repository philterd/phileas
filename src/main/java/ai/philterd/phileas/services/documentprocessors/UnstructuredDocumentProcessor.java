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
import ai.philterd.phileas.model.filtering.Explanation;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.IncrementalRedaction;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationService;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.tokens.TokenCounter;
import ai.philterd.phileas.services.tokens.WhitespaceTokenCounter;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
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
    public TextFilterResult process(final Policy policy, final List<Filter> filters, final List<PostFilter> postFilters,
                                    final String context, final int piece, final String input) throws Exception {

        // The list that will contain the spans containing PHI/PII.
        List<Span> identifiedSpans = new LinkedList<>();

        // Apply each filter.
        for(final Filter filter : filters) {

            final Filtered filtered = filter.filter(policy, context, piece, input);
            identifiedSpans.addAll(filtered.getSpans());

        }

        // Perform span disambiguation. When disabled, this is a no-op implementation that returns
        // the spans unchanged (see SpanDisambiguationServiceFactory), so no enabled-check is needed.
        identifiedSpans = spanDisambiguationService.disambiguate(context, identifiedSpans);

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

        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();
        final long tokens = tokenCounter.countTokens(input);

        // Apply the replacements in ascending start order, tracking the cumulative offset introduced
        // by replacements whose length differs from the original span. This replaces the previous
        // character-by-character scan (which called Span.doesIndexStartSpan - a linear scan of the
        // span list - for every character of the document, i.e. O(documentLength x spans)) and the
        // per-replacement Span.shiftSpans rebuild of the entire span list. The spans do not overlap
        // (overlaps were removed earlier by Span.dropOverlappingSpans), so this single left-to-right
        // pass is safe and preserves the original replacement order. appliedSpans is left unmodified
        // so the Explanation keeps the original span locations.
        final List<Span> orderedSpans = appliedSpans.stream()
                .sorted(Comparator.comparingInt(Span::getCharacterStart))
                .collect(toList());

        int offset = 0;

        for(final Span span : orderedSpans) {

            // Get the replacement. This might be the token itself or an anonymized version.
            final String replacement = span.getReplacement();

            final int start = span.getCharacterStart() + offset;
            final int end = span.getCharacterEnd() + offset;

            // Do the replacement at the offset-adjusted position.
            sb.replace(start, end, replacement);

            // A replacement longer than the original shifts later spans right; shorter shifts left.
            offset += replacement.length() - (span.getCharacterEnd() - span.getCharacterStart());

            if(incrementalRedactionsEnabled) {
                // Hash the text at this point.
                final String snapshot = sb.toString();
                incrementalRedactions.add(new IncrementalRedaction(DigestUtils.sha256Hex(snapshot), span, snapshot));
            }

        }

        return new TextFilterResult(sb.toString(), context, piece, explanation, incrementalRedactions, tokens);

    }

}
