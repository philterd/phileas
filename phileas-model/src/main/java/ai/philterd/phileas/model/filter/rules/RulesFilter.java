/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.model.objects.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Policy;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
     * @param filterType The @{link FilterType} of the filter.
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     */
    public RulesFilter(final FilterType filterType, final FilterConfiguration filterConfiguration) {
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
    public List<Span> postFilter(final List<Span> spans) {
        return spans;
    }

    /**
     * Find {@link Span spans} matching the {@link Pattern}.
     * @param policy The {@link Policy} to use.
     * @param analyzer A filter {@link Analyzer}.
     * @param input The text input.
     * @param context The context.
     * @param documentId The document ID.
     * @param attributes Attributes about the input text.
     * @return A list of matching {@link Span spans}.
     */
    protected List<Span> findSpans(final Policy policy, final Analyzer analyzer, final String input, final String context,
                                   final String documentId, final Map<String, String> attributes) throws Exception {

        final List<Span> spans = new LinkedList<>();

        // Is this filter enabled for this policy? If not just return empty list.
        if(policy.getIdentifiers().hasFilter(filterType)) {

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
                        final Replacement replacement = getReplacement(policy, context, documentId, token,
                                window, initialConfidence, classification, attributes, filterPattern);

                        // Create the span.
                        final Span span = Span.make(characterStart, characterEnd, getFilterType(), context, documentId,
                                initialConfidence, token, replacement.getReplacement(), replacement.getSalt(),
                                ignored, replacement.isApplied(), window);

                        // TODO: Add "format" to Span.make() so we don't have to make a separate call here.
                        span.setPattern(filterPattern.getFormat());

                        // TODO: Add "alwaysValid" to Span.make() so we don't have to make a separate call here.
                        span.setAlwaysValid(filterPattern.isAlwaysValid());

                        // TODO: Add "classification" to Span.make() so we don't have to make a separate call here.
                        span.setClassification(filterPattern.getClassification());

                        spans.add(span);

                    }

                }

            }

        }

        return postFilter(spans);
        
    }

    /**
     * Gets the count of occurrences.
     * @param policy The {@link Policy} to use.
     * @param input The input text.
     * @return A count of occurrences in the text.
     */
    @Override
    public int getOccurrences(final Policy policy, final String input, final Map<String, String> attributes) throws Exception {

        return filter(policy, "none", "none", 0, input, attributes).getSpans().size();

    }

}
