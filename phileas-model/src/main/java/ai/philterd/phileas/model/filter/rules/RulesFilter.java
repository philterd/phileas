/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.filter.rules;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.Filter;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.*;
import ai.philterd.phileas.model.profile.FilterProfile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for rules-based filters.
 */
public abstract class RulesFilter extends Filter {

    protected Set<String> contextualTerms;

    /**
     * Creates a new rule-based filter.
     * @param filterType
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     */
    public RulesFilter(FilterType filterType, FilterConfiguration filterConfiguration) {
        super(filterType, filterConfiguration);
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
    protected List<Span> findSpans(FilterProfile filterProfile, Analyzer analyzer, String input, String context,
                                   String documentId) throws Exception {
        return findSpans(filterProfile, analyzer, input, context, documentId, Collections.emptyMap());
    }

    /**
     * Find {@link Span spans} matching the {@link Pattern}.
     * @param filterProfile The {@link FilterProfile} to use.
     * @param analyzer A filter {@link Analyzer}.
     * @param input The text input.
     * @param context The context.
     * @param documentId The document ID.
     * @param restrictions Restrictions placed on what is a span for the filter.
     * @return A list of matching {@link Span spans}.
     */
    protected List<Span> findSpans(FilterProfile filterProfile, Analyzer analyzer, String input, String context,
                                   String documentId, Map<Restriction, List<String>> restrictions) throws Exception {

        final List<Span> spans = new LinkedList<>();

        // Is this filter enabled for this filter profile? If not just return empty list.
        if(filterProfile.getIdentifiers().hasFilter(filterType)) {

            for(final FilterPattern filterPattern : analyzer.getFilterPatterns()) {

                final Matcher matcher = filterPattern.getPattern().matcher(input);

                while (matcher.find()) {

                    final String token = matcher.group(filterPattern.getGroupNumber());

                    // The token's position inside the text.
                    final int characterStart = matcher.start(filterPattern.getGroupNumber());
                    final int characterEnd = matcher.end(filterPattern.getGroupNumber());

                    // Is there already a span encompassing this location?
                    // If so just quit. This means the first match wins.
                    if(!Span.doesSpanExist(characterStart, characterEnd, spans)) {

                        // Is this term ignored?
                        boolean ignored = isIgnored(token);

                        // TODO: PHL-119: Adjust the confidence based on the initial confidence.
                        // TODO: Should this be an option? Use "simple" confidence values or "calculated"?
                        final double initialConfidence = filterPattern.getInitialConfidence();

                        // Get the window of words around the token.
                        final String[] window = getWindow(input, characterStart, characterEnd);

                        // Get the span's replacement.
                        final Replacement replacement = getReplacement(filterProfile.getName(), context, documentId, token,
                                window, initialConfidence, filterPattern.getClassification(), filterPattern);

                        final Span span = Span.make(characterStart, characterEnd, getFilterType(), context, documentId,
                                initialConfidence, token, replacement.getReplacement(), replacement.getSalt(), ignored, window);

                        // TODO: Add "format" to Span.make() so we don't have to make a separate call here.
                        span.setPattern(filterPattern.getFormat());

                        // TODO: Add "classification" to Span.make() so we don't have to make a separate call here.
                        span.setClassification(filterPattern.getClassification());

                        // TODO: Add "alwaysValid" to Span.make() so we don't have to make a separate call here.
                        span.setAlwaysValid(filterPattern.isAlwaysValid());

                        // Lastly, look at any restrictions before adding the span.
                        boolean restricted = false;
                        for(final Restriction restriction : restrictions.keySet()) {
                            if(restriction == Restriction.CLASSIFICATION) {
                                if(!restrictions.get(restriction).contains(filterPattern.getClassification())) {
                                    restricted = true;
                                }
                            }
                        }

                        if(!restricted) {
                            spans.add(span);
                        }

                    }

                }

            }

        }

        return postFilter(spans);
        
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
