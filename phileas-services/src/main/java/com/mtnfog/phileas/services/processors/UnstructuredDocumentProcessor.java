package com.mtnfog.phileas.services.processors;

import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.Explanation;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.services.DocumentProcessor;
import com.mtnfog.phileas.model.services.MetricsService;
import com.mtnfog.phileas.model.services.PostFilter;
import com.mtnfog.phileas.model.services.Store;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Processes and filters unstructured text documents.
 */
public class UnstructuredDocumentProcessor implements DocumentProcessor {

    private Map<String, List<Filter>> filters;
    private List<PostFilter> postFilters;
    private MetricsService metricsService;
    private Store store;

    public UnstructuredDocumentProcessor(Map<String, List<Filter>> filters, List<PostFilter> postFilters, MetricsService metricsService, Store store) {

        this.filters = filters;
        this.postFilters = postFilters;
        this.metricsService = metricsService;
        this.store = store;

    }

    @Override
    public FilterResponse process(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        // The list that will contain the spans containing PHI/PII.
        List<Span> spans = new LinkedList<>();

        // Get the enabled filters for this filter profile.
        final List<Filter> allFiltersFromProfile = filters.get(filterProfile.getName());

        // Execute each filter.
        for(final Filter filter : allFiltersFromProfile) {
            spans.addAll(filter.filter(filterProfile, context, documentId, input));
        }

        // Drop overlapping spans.
        spans = Span.dropOverlappingSpans(spans);

        // Drop ignored spans.
        spans = Span.dropIgnoredSpans(spans);

        // Sort the spans based on the confidence.
        spans.sort(Comparator.comparing(Span::getConfidence));

        // Perform post-filtering on the spans.
        for(final PostFilter postFilter : postFilters) {
            spans = postFilter.filter(input, spans);
        }

        // The spans that will be persisted. Has to be a deep copy because the shift
        // below will change the indexes. Doing this to save the original locations of the spans.
        final List<Span> appliedSpans = spans.stream().map(d -> d.copy()).collect(toList());

        // TODO: Set a flag on each "span" not in appliedSpans indicating it was not used.

        // Log a metric for each filter type.
        appliedSpans.forEach(k -> metricsService.incrementFilterType(k.getFilterType()));

        // Define the explanation.
        final Explanation explanation = new Explanation(appliedSpans, spans);

        // Used to manipulate the text.
        final StringBuffer buffer = new StringBuffer(input);

        // Initialize this to the the input length but it may grow in length if redactions/replacements
        // are longer than the original spans.
        int stringLength = input.length();

        // Go character by character through the input.
        for(int i = 0; i < stringLength; i++) {

            // Is index i the start of a span?
            final Span span = Span.doesIndexStartSpan(i, spans);

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
                    spans = Span.shiftSpans(shift, span, spans);

                    // Update the length of the string.
                    stringLength += shift;

                }

                // We can now do the replacement.
                buffer.replace(span.getCharacterStart(), span.getCharacterEnd(), replacement);

                // Jump ahead outside of this span.
                i = span.getCharacterEnd();

            }

        }

        metricsService.incrementProcessed();

        // Store the applied spans in the database.
        if(store != null) {
            store.insert(appliedSpans);
        }

        return new FilterResponse(buffer.toString(), context, documentId, explanation);

    }

}