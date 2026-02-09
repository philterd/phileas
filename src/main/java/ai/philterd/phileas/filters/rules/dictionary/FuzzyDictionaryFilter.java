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
package ai.philterd.phileas.filters.rules.dictionary;

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Position;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.model.filtering.SensitivityLevel;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FuzzyDictionaryFilter extends DictionaryFilter {

    private final SensitivityLevel sensitivityLevel;
    private final Map<String, Pattern> dictionary;
    private final int maxNgrams;
    private final boolean requireCapitalization;

    public FuzzyDictionaryFilter(final FilterType filterType, final FilterConfiguration filterConfiguration,
                                 final SensitivityLevel sensitivityLevel, final boolean requireCapitalization) throws IOException {
        super(filterType, filterConfiguration);

        this.sensitivityLevel = sensitivityLevel;
        this.dictionary = loadData(filterType);
        this.maxNgrams = getMaxNgrams();
        this.requireCapitalization = requireCapitalization;

    }

    public FuzzyDictionaryFilter(final FilterType filterType, final FilterConfiguration filterConfiguration,
                                 final SensitivityLevel sensitivityLevel, final Set<String> terms, final boolean requireCapitalization) {
        super(filterType, filterConfiguration);

        this.sensitivityLevel = sensitivityLevel;
        this.dictionary = loadData(terms);
        this.maxNgrams = getMaxNgrams();
        this.requireCapitalization = requireCapitalization;

    }

    @Override
    public Filtered filter(Policy policy, final String context, int piece, String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        if(policy.getIdentifiers().hasFilter(filterType)) {

            // Build ngrams from the input text.
            final Map<Integer, Map<Position, String>> ngrams = new HashMap<>();
            ngrams.put(0, splitWithIndexes(input, " "));

            for(int x = 1; x < maxNgrams; x++) {
                ngrams.put(x, getNgramsOfLength(input, x));
            }

            for(final String entry : dictionary.keySet()) {

                final Matcher matcher = dictionary.get(entry).matcher(input);

                // Exact matches.
                if (matcher.find()) {

                    final int startPosition = matcher.start();
                    spans.add(createSpan(input, startPosition, startPosition + entry.length(), 1.0, context, entry, policy));

                } else {

                    // Only when the sensitivity level is not "off".
                    if(sensitivityLevel != SensitivityLevel.OFF) {

                        // Fuzzy matches.
                        final int spacesInEntry = StringUtils.countMatches(entry, " ");

                        for (final Position position : ngrams.get(spacesInEntry).keySet()) {

                            // Compare string distance between word and ngrams.
                            final String ngram = ngrams.get(spacesInEntry).get(position);

                            if (ngram.length() > 2) {

                                if (requireCapitalization && Character.isUpperCase(ngram.charAt(0))) {

                                    final int start = position.getStart();
                                    final int end = position.getEnd();

                                    // TODO: Should this be customizable in the dictionary's properties in the filter policy?
                                    final LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
                                    final int distance = levenshteinDistance.apply(entry, ngram);

                                    if (sensitivityLevel == SensitivityLevel.HIGH && distance < 1) {
                                        spans.add(createSpan(input, start, end, 0.9, context, entry, policy));
                                    } else if (sensitivityLevel == SensitivityLevel.MEDIUM && distance <= 2) {
                                        spans.add(createSpan(input, start, end, 0.5, context, entry, policy));
                                    } else if (sensitivityLevel == SensitivityLevel.LOW && distance < 3) {
                                        spans.add(createSpan(input, start, end, 0.3, context, entry, policy));
                                    }

                                }

                            }

                        }

                    }

                }

            }

        }

        return new Filtered(context, spans);

    }

    private Span createSpan(String text, int characterStart, int characterEnd, double confidence, String context,
                            String token, Policy policy) throws Exception {

        final boolean ignored = isIgnored(text);
        final String[] window = getWindow(text, characterStart, characterEnd);

        // Get the replacement token or the original token if no filter strategy conditions are met.
        final Replacement replacement = getReplacement(policy, context, token,
                window, confidence, classification, null);

        // Add the span to the list.
        return Span.make(characterStart, characterEnd, getFilterType(), context,
                confidence, token, replacement.getReplacement(),
                replacement.getSalt(), ignored, replacement.isApplied(), window, priority);

    }

    private int getMaxNgrams() {

        // Get the max number of n-grams to break the text up into based on the
        // max number of spaces in any individual entry in the dictionary, up to
        // a max of 20.
        // TODO: Externalize the limit of 20.

        int maxNgrams = 0;

        for(final String key : dictionary.keySet()) {
            final int n = key.split(" ").length;
            if(n > maxNgrams) {
                maxNgrams = n;
            }

            if(n >= 20) {
                break;
            }
        }

        return maxNgrams;

    }

    private Map<String, Pattern> loadData(final Set<String> terms) {

        final Map<String, Pattern> dictionary = new HashMap<>();

        for(final String term : terms) {
                final Pattern pattern = Pattern.compile("\\b" + term + "\\b", Pattern.CASE_INSENSITIVE);
            dictionary.put(term, pattern);
        }

        return dictionary;

    }

}

