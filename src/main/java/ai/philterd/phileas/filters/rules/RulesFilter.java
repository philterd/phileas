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
package ai.philterd.phileas.filters.rules;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.services.Analyzer;
import ai.philterd.phileas.model.objects.ConfidenceModifier;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.Position;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.policy.Policy;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
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
     * @param attributes Attributes about the input text.
     * @return A list of matching {@link Span spans}.
     */
    protected List<Span> findSpans(final Policy policy, final Analyzer analyzer, final String input, final String context) throws Exception {

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
                        double initialConfidence = filterPattern.getInitialConfidence();

                        // Look at the ConfidenceModifiers (if there are any).
                        if(CollectionUtils.isNotEmpty(filterPattern.getConfidenceModifiers())) {

                            for(final ConfidenceModifier modifier : filterPattern.getConfidenceModifiers()) {

                                if(modifier.getConfidenceCondition() == ConfidenceModifier.ConfidenceCondition.CHARACTER_SEQUENCE_BEFORE) {
                                    if(characterStart > 0) {
                                        if (StringUtils.equalsIgnoreCase(String.valueOf(input.charAt(characterStart - 1)), modifier.getCharacters())) {
                                            if(modifier.getConfidenceDelta() != 0) {
                                                initialConfidence += modifier.getConfidenceDelta();
                                            } else {
                                                initialConfidence = modifier.getConfidence();
                                            }
                                        }
                                    }
                                }

                                if(modifier.getConfidenceCondition() == ConfidenceModifier.ConfidenceCondition.CHARACTER_SEQUENCE_AFTER) {
                                    if(characterEnd < input.length()) {
                                        if (StringUtils.equalsIgnoreCase(String.valueOf(input.charAt(characterEnd)), modifier.getCharacters())) {
                                            if(modifier.getConfidenceDelta() != 0) {
                                                initialConfidence += modifier.getConfidenceDelta();
                                            } else {
                                                initialConfidence = modifier.getConfidence();
                                            }
                                        }
                                    }
                                }

                                if(modifier.getConfidenceCondition() == ConfidenceModifier.ConfidenceCondition.CHARACTER_SEQUENCE_SURROUNDING) {
                                    if(characterStart > 0 && characterEnd < input.length()) {
                                        if (StringUtils.equalsIgnoreCase(String.valueOf(input.charAt(characterStart - 1)), modifier.getCharacters())) {
                                            if (StringUtils.equalsIgnoreCase(String.valueOf(input.charAt(characterEnd)), modifier.getCharacters())) {
                                                if(modifier.getConfidenceDelta() != 0) {
                                                    initialConfidence += modifier.getConfidenceDelta();
                                                } else {
                                                    initialConfidence = modifier.getConfidence();
                                                }
                                            }
                                        }
                                    }
                                }

                                if(modifier.getConfidenceCondition() == ConfidenceModifier.ConfidenceCondition.CHARACTER_REGEX_SURROUNDING) {
                                    if(characterStart > 0 && characterEnd < input.length()) {
                                        if (modifier.getMatchingPattern().matcher(input.substring(characterStart - 1, characterStart)).matches()) {
                                            if (modifier.getMatchingPattern().matcher(input.substring(characterEnd, characterEnd + 1)).matches()) {
                                                if(modifier.getConfidenceDelta() != 0) {
                                                    initialConfidence += modifier.getConfidenceDelta();
                                                } else {
                                                    initialConfidence = modifier.getConfidence();
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Make sure the confidence is between 0 and 1.
                            if(initialConfidence < 0) {
                                initialConfidence = 0;
                            }

                            if(initialConfidence > 1) {
                                initialConfidence = 1;
                            }

                        }

                        // Get the window of words around the token.
                        final String[] window = getWindow(input, characterStart, characterEnd);

                        // Get the span's replacement.
                        final Replacement replacement = getReplacement(policy, context, token,
                                window, initialConfidence, classification, filterPattern);

                        // Create the span.
                        final Span span = Span.make(characterStart, characterEnd, getFilterType(), context,
                                initialConfidence, token, replacement.getReplacement(), replacement.getSalt(),
                                ignored, replacement.isApplied(), window, priority);

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
     * Get n-grams from text up to a given length.
     * @param text The text from which to extract n-grams.
     * @param length The max length of n-grams to extract.
     * @return N-grams as a map of {@link Position} to the n-gram itself.
     */
    public Map<Position, String> getNgramsUpToLength(String text, int length) {

        final Map<Position, String> ngrams = new HashMap<>();

        for(int n = length; n > 0; n--) {
            ngrams.putAll(getNgramsOfLength(text, n));
        }

        return ngrams;

    }

    /**
     * Get n-grams from text having a given length.
     * @param text The text from which to extract n-grams.
     * @param length The length of n-grams to extract.
     * @return N-grams as a map of {@link Position} to the n-gram itself.
     */
    public Map<Position, String> getNgramsOfLength(String text, int length) {

        final String delimiter = " ";

        final Map<Position, String> ngramsWithIndexes = new HashMap<>();
        final String[] words = text.split(delimiter);
        int lastLocation = 0;

        for (int i = 0; i <= words.length - length; i++) {

            final StringBuilder ngram = new StringBuilder();

            for (int j = 0; j < length; j++) {

                ngram.append(words[i + j]);

                if (j < length - 1) {
                    ngram.append(" ");
                }

            }

            int newLocation = text.indexOf(ngram.toString(), lastLocation);
            lastLocation = newLocation;

            final Position position = new Position(newLocation, newLocation + ngram.toString().length());

            ngramsWithIndexes.put(position, ngram.toString());

        }

        return ngramsWithIndexes;

    }

}
