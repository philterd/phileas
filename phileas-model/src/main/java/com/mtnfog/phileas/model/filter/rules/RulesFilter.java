package com.mtnfog.phileas.model.filter.rules;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for rules-based filters.
 */
public abstract class RulesFilter extends Filter implements Serializable {

    /**
     * Creates a new rule-based filter.
     * @param filterType The {@link FilterType type} of the filter.
     * @param anonymizationService The {@link AnonymizationService} for this filter.
     */
    public RulesFilter(FilterType filterType, List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, Set<String> ignored, Crypto crypto) {
        super(filterType, strategies, anonymizationService, ignored, crypto);
    }

    /**
     * Find {@link Span spans} matching the {@link Pattern}.
     * @param filterProfile The {@link FilterProfile} to use.
     * @param pattern The regex {@link Pattern}.
     * @param input The text input.
     * @param context The context.
     * @param documentId The document ID.
     * @return A list of matching {@link Span spans}.
     */
    protected List<Span> findSpans(FilterProfile filterProfile, Pattern pattern, String input, String context, String documentId) throws Exception {

        final List<Span> spans = new LinkedList<>();

        // Is this filter enabled for this filter profile? If not just return empty list.
        if(filterProfile.getIdentifiers().hasFilter(filterType)) {

            final Matcher matcher = pattern.matcher(input);

            while (matcher.find()) {

                final String token = matcher.group(0);

                // Is this term ignored?
                boolean isIgnored = ignored.contains(token);

                // There are no attributes for the span.
                final String replacement = getReplacement(label, context, documentId, token, Collections.emptyMap());

                final Span span = Span.make(matcher.start(0), matcher.end(0), getFilterType(), context, documentId, 1.0, token, replacement, isIgnored);

                spans.add(span);

            }

        }

        return spans;

    }

    /**
     * Gets the count of occurrences.
     * @param filterProfile The {@link FilterProfile} to use.
     * @param input The input text.
     * @return A count of occurrences in the text.
     */
    public int getOccurrences(FilterProfile filterProfile, String input) throws Exception {

        return filter(filterProfile, "none", "none", input).size();

    }

}
