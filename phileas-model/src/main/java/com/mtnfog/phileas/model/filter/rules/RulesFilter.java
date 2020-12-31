package com.mtnfog.phileas.model.filter.rules;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.IgnoredPattern;
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
    public RulesFilter(FilterType filterType, List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Set<String> ignoredFiles, List<IgnoredPattern> ignoredPatterns, Crypto crypto, int windowSize) {
        super(filterType, strategies, anonymizationService, alertService, ignored, ignoredFiles, ignoredPatterns, crypto, windowSize);
    }

    // TODO: Move this to Filter.java to all filters can have access to it.
    /**
     * Provides the ability to apply a post filter to identified spans.
     * This can be used to remove false positives.
     * Override this function in subclasses.
     * @param spans The identified spans.
     * @return A subset of the input spans.
     */
    public List<Span> postFilter(List<Span> spans) {
        return spans;
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
                    boolean ignored = isIgnored(token);

                    // TODO: PHL-119: Adjust the confidence based on the initial confidence.
                    // TODO: Should this be an option? Use "simple" confidence values or "calculated"?
                    final double initialConfidence = filterPattern.getInitialConfidence();

                    // The token's position inside the text.
                    final int characterStart = matcher.start(0);
                    final int characterEnd = matcher.end(0);

                    // Get the window of words around the token.
                    final String[] window = getWindow(input, characterStart, characterEnd);

                    // Get the span's replacement.
                    final Replacement replacement = getReplacement(filterProfile.getName(), context, documentId, token, window, initialConfidence, filterPattern.getClassification(), filterPattern);

                    final Span span = Span.make(characterStart, characterEnd, getFilterType(), context, documentId, initialConfidence, token, replacement.getReplacement(), replacement.getSalt(), ignored, window);

                    // TODO: Add "format" to Span.make() so we don't have to make a separate call here.
                    span.setPattern(filterPattern.getFormat());

                    // TODO: Add "classification" to Span.make() so we don't have to make a separate call here.
                    span.setClassification(filterPattern.getClassification());

                    spans.add(span);

                }

            }

        }

        final List<Span> postFilteredSpans = postFilter(spans);

        return postFilteredSpans;

    }

    /**
     * Gets the count of occurrences.
     * @param filterProfile The {@link FilterProfile} to use.
     * @param input The input text.
     * @return A count of occurrences in the text.
     */
    @Override
    public int getOccurrences(FilterProfile filterProfile, String input) throws Exception {

        return filter(filterProfile, "none", "none", 0, input).getSpans().size();

    }

}
