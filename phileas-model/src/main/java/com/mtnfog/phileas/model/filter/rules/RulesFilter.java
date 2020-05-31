package com.mtnfog.phileas.model.filter.rules;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for rules-based filters.
 */
public abstract class RulesFilter extends Filter {

    protected Set<String> contextualTerms;

    /**
     * Creates a new rule-based filter.
     * @param filterType The {@link FilterType type} of the filter.
     * @param anonymizationService The {@link AnonymizationService} for this filter.
     */
    public RulesFilter(FilterType filterType, List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(filterType, strategies, anonymizationService, alertService, ignored, crypto, windowSize);
    }

    /**
     * Find {@link Span spans} matching the {@link Pattern}.
     * @param filterProfile The {@link FilterProfile} to use.
     * @param analyzer A filter {@link Analyzer}.
     * @param input The text input.
     * @param context The context.
     * @param documentId The document ID.
     * @return A list of matching {@link Span spans}.
     */
    protected List<Span> findSpans(FilterProfile filterProfile, Analyzer analyzer, String input, String context, String documentId) throws Exception {

        final List<Span> spans = new LinkedList<>();

        // Is this filter enabled for this filter profile? If not just return empty list.
        if(filterProfile.getIdentifiers().hasFilter(filterType)) {

            for(final FilterPattern filterPattern : analyzer.getFilterPatterns()) {

                final Matcher matcher = filterPattern.getPattern().matcher(input);

                while (matcher.find()) {

                    final String token = matcher.group(0);

                    // Is this term ignored?
                    final boolean isIgnored = ignored.contains(token);

                    // TODO: PHL-119: Adjust the confidence based on the initial confidence.
                    // TODO: Should this be an option? Use "simple" confidence values or "calculated"?
                    final double initialConfidence = filterPattern.getInitialConfidence();

                    // Get the span's replacement.
                    final Replacement replacement = getReplacement(filterProfile.getName(), context, documentId, token, initialConfidence, filterPattern.getClassification());

                    final int characterStart = matcher.start(0);
                    final int characterEnd = matcher.end(0);

                    final String[] window = getWindow(input, characterStart, characterEnd);

                    final Span span = Span.make(characterStart, characterEnd, getFilterType(), context, documentId, initialConfidence, token, replacement.getReplacement(), replacement.getSalt(), isIgnored, window);

                    // TODO: Add "format" to Span.make() so we don't have to make a separate call here.
                    span.setPattern(filterPattern.getFormat());

                    // TODO: Add "classification" to Span.make() so we don't have to make a separate call here.
                    span.setClassification(filterPattern.getClassification());

                    spans.add(span);

                }

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
