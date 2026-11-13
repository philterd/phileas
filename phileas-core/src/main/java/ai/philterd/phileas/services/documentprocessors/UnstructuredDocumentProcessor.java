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
import ai.philterd.phileas.policy.config.Analysis;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
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

    /** A missing analysis section means the default, enabled. */
    private static boolean disambiguationAllowed(final Policy policy) {
        final Analysis analysis = policy.getConfig().getAnalysis();
        return analysis == null || analysis.isSpanDisambiguation();
    }

    @Override
    public TextFilterResult process(final ContextService contextService, final VectorService vectorService,
                                    final Policy policy, final List<Filter> filters, final List<PostFilter> postFilters,
                                    final String context, final int piece, final String input) throws Exception {

        final List<Span> identifiedSpans = detect(contextService, policy, filters, context, piece, input);

        return apply(vectorService, policy, postFilters, context, piece, input, identifiedSpans);

    }

    /**
     * Runs the filters over one piece of text.
     *
     * @return The spans found, with offsets relative to the given text.
     */
    public List<Span> detect(final ContextService contextService, final Policy policy, final List<Filter> filters,
                             final String context, final int piece, final String input) throws Exception {

        final List<Span> identifiedSpans = new LinkedList<>();

        for(final Filter filter : filters) {

            final Filtered filtered = filter.filter(contextService, policy, context, piece, input);
            identifiedSpans.addAll(filtered.getSpans());

        }

        return identifiedSpans;

    }

    /**
     * Disambiguates, de-duplicates and post-filters the given spans, then applies them to the text.
     * The span offsets must be relative to the text given here.
     */
    public TextFilterResult apply(final VectorService vectorService, final Policy policy,
                                  final List<PostFilter> postFilters, final String context, final int piece,
                                  final String input, final List<Span> spans) throws Exception {

        List<Span> identifiedSpans = spans;

        // A policy can turn this off but not on: the service is a no-op when off globally.
        if(disambiguationAllowed(policy)) {
            identifiedSpans = spanDisambiguationService.disambiguate(vectorService, context, identifiedSpans);
        }

        // Resolve overlapping spans.
        identifiedSpans = Span.resolveOverlappingSpans(identifiedSpans, input);

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

        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();
        final long tokens = tokenCounter.countTokens(input);

        // The spans do not overlap (resolveOverlappingSpans, and post filters only shorten spans), so the
        // output is built in one pass. appliedSpans keeps the original locations for the Explanation.
        final List<Span> orderedSpans = appliedSpans.stream()
                .sorted(Comparator.comparingInt(Span::getCharacterStart))
                .collect(toList());

        final StringBuilder sb = new StringBuilder(input.length());
        int copiedTo = 0;

        for(final Span span : orderedSpans) {

            // Get the replacement. This might be the token itself or an anonymized version.
            final String replacement = span.getReplacement();

            sb.append(input, copiedTo, span.getCharacterStart()).append(replacement);
            copiedTo = span.getCharacterEnd();

            if(incrementalRedactionsEnabled) {
                // Hash the text at this point: the replacements so far and the rest of the input.
                final String snapshot = sb + input.substring(copiedTo);
                incrementalRedactions.add(new IncrementalRedaction(DigestUtils.sha256Hex(snapshot), span, snapshot));
            }

        }

        sb.append(input, copiedTo, input.length());

        return new TextFilterResult(sb.toString(), context, piece, explanation, incrementalRedactions, tokens);

    }

}
