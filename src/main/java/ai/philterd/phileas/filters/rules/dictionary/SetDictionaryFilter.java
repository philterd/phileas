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
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A dictionary filter that matches terms by exact, case-insensitive lookup in a set.
 *
 * <p>The backing lookup is an in-memory {@link Set}, which is already O(1), so this filter
 * intentionally does not use a bloom pre-filter: a bloom filter cannot make an O(1) set lookup
 * faster — it only adds work (hashing every token) and memory (a second structure) — regardless of
 * how many terms the dictionary contains. It is used for both custom dictionaries and the
 * predefined file-based dictionaries (cities, counties, names, and so on) when fuzzy matching is
 * not enabled.</p>
 */
public class SetDictionaryFilter extends DictionaryFilter {

    private final Set<String> lowerCaseTerms;
    private int maxNgramSize = 0;

    /**
     * Creates a new set-based dictionary filter whose terms are loaded from the dictionary file
     * bundled for the given {@link FilterType} (for example the predefined city, county, or name
     * dictionaries).
     * @param filterType The {@link FilterType type} of the filter.
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     * @throws IOException If the dictionary file cannot be read.
     */
    public SetDictionaryFilter(final FilterType filterType,
                               final FilterConfiguration filterConfiguration) throws IOException {

        super(filterType, filterConfiguration);

        // Reuse the shared, process-wide predefined dictionary so the large term set is not
        // duplicated per FilterService instance.
        final PredefinedDictionary dictionary = getPredefinedDictionary(filterType);
        this.lowerCaseTerms = dictionary.lowerCaseTerms();
        this.maxNgramSize = dictionary.maxNgramSize();

    }

    /**
     * Creates a new set-based dictionary filter.
     * @param filterType The {@link FilterType type} of the filter.
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     * @param terms A set of terms that will be in the dictionary.
     * @param classification A classification label for the type of information.
     */
    public SetDictionaryFilter(final FilterType filterType,
                               final FilterConfiguration filterConfiguration,
                               final Set<String> terms,
                               final String classification) {

        super(filterType, filterConfiguration);

        this.lowerCaseTerms = new HashSet<>();
        this.classification = classification;

        // Lowercase each term and find the max n-gram size, which is the maximum number of
        // whitespace-separated words in any single dictionary entry.
        for(final String term : terms) {
            final String[] split = term.split("\\s");
            if(split.length > maxNgramSize) {
                maxNgramSize = split.length;
            }
            lowerCaseTerms.add(term.toLowerCase());
        }

    }

    @Override
    public Filtered filter(final Policy policy, final String context, final int piece, final String text) throws Exception {

        final List<Span> spans = new LinkedList<>();

        final Map<Position, String> ngrams = getNgramsUpToLength(text, maxNgramSize);

        for(final Position position : ngrams.keySet()) {

            final String ngram = ngrams.get(position);

            // Match the token as-is, or with surrounding (leading/trailing) punctuation removed so a
            // term is still found when it is adjacent to punctuation — for example "Boston" in the
            // text "...visited Boston." where the whitespace tokenizer produces the token "Boston.".
            // begin/end bound the matched portion within the token so the resulting span covers only
            // the term, not the punctuation.
            int begin = 0;
            int end = ngram.length();
            boolean matched = lowerCaseTerms.contains(ngram.toLowerCase());

            if (!matched) {
                while (begin < end && !Character.isLetterOrDigit(ngram.charAt(begin))) {
                    begin++;
                }
                while (end > begin && !Character.isLetterOrDigit(ngram.charAt(end - 1))) {
                    end--;
                }
                if (begin != 0 || end != ngram.length()) {
                    matched = lowerCaseTerms.contains(ngram.substring(begin, end).toLowerCase());
                }
            }

            if (matched) {

                final int characterStart = position.getStart() + begin;
                final int characterEnd = position.getStart() + end;

                // Get the original token to get the right casing.
                final String originalToken = text.substring(characterStart, characterEnd);

                // Set the meta values for the span.
                final boolean isIgnored = ignored.contains(originalToken);
                final double confidence = 1.0;
                final String[] window = getWindow(text, characterStart, characterEnd);

                final Replacement replacement = getReplacement(policy, context,
                        originalToken, window, confidence, classification, null);

                spans.add(Span.make(characterStart, characterEnd, getFilterType(), context,
                        confidence, originalToken, replacement.getReplacement(), replacement.getSalt(),
                        isIgnored, replacement.isApplied(), window, priority));

            }

        }

        return new Filtered(context, spans);

    }

}
