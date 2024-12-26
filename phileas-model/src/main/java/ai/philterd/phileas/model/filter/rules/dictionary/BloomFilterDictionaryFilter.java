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
package ai.philterd.phileas.model.filter.rules.dictionary;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Position;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.utils.BloomFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A filter that operates on a bloom filter.
 */
public class BloomFilterDictionaryFilter extends DictionaryFilter {

    private static final Logger LOGGER = LogManager.getLogger(BloomFilterDictionaryFilter.class);

    private final BloomFilter<String> bloomFilter;
    private final Set<String> lowerCaseTerms;
    private int maxNgramSize = 0;

    /**
     * Creates a new bloom filter-based filter.
     * @param filterType The {@link FilterType type} of the filter.
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     * @param terms A set of terms that will be in the dictionary.
     * @param classification A classification label for the type of information.
     */
    public BloomFilterDictionaryFilter(FilterType filterType,
                                       FilterConfiguration filterConfiguration,
                                       Set<String> terms,
                                       String classification) {

        super(filterType, filterConfiguration);

        this.lowerCaseTerms = new HashSet<>();
        this.bloomFilter = new BloomFilter<>(terms.size());
        this.classification = classification;

        // Find the max n-gram size. It is equal to the maximum number of spaces in any single dictionary entry.
        for(final String term : terms) {
            final String[] split = term.split("\\s");
            if(split.length > maxNgramSize) {
                maxNgramSize = split.length;
            }
        }

        // Lowercase the terms and add each to the bloom filter.
         for(final String term : terms) {
            lowerCaseTerms.add(term.toLowerCase());
            bloomFilter.put(term.toLowerCase());
        }

    }

    @Override
    public FilterResult filter(Policy policy, String context, String documentId, int piece, String text,
                               Map<String, String> attributes) throws Exception {

        final List<Span> spans = new LinkedList<>();

        final Map<Position, String> ngrams = getNgramsUpToLength(text, maxNgramSize);

        for(final Position position : ngrams.keySet()) {

            final String ngram = ngrams.get(position);

            if (bloomFilter.mightContain(ngram.toLowerCase())) {

                if (lowerCaseTerms.contains(ngram.toLowerCase())) {

                    // Set the meta values for the span.
                    final boolean isIgnored = ignored.contains(ngram);

                    final int characterStart = position.getStart();
                    final int characterEnd = position.getEnd();
                    final double confidence = 1.0;
                    final String[] window = getWindow(text, characterStart, characterEnd);

                    // Get the original token to get the right casing.
                    final String originalToken = text.substring(characterStart, characterEnd);

                    final Replacement replacement = getReplacement(policy, context, documentId,
                            originalToken, window, confidence, classification, attributes, null);

                    spans.add(Span.make(characterStart, characterEnd, getFilterType(), context, documentId,
                            confidence, originalToken, replacement.getReplacement(), replacement.getSalt(),
                            isIgnored, replacement.isApplied(), window));

                }

            }

        }

        return new FilterResult(context, documentId, spans);

    }

}
