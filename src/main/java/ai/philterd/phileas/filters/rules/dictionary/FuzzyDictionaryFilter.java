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
import ai.philterd.phileas.services.context.ContextService;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.IOException;
import java.util.ArrayList;
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
        this(filterType, filterConfiguration, sensitivityLevel, terms, requireCapitalization, null);
    }

    /**
     * Creates a new fuzzy dictionary filter.
     * @param filterType The {@link FilterType type} of the filter.
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     * @param sensitivityLevel The {@link SensitivityLevel} for fuzzy matching.
     * @param terms A set of terms that will be in the dictionary.
     * @param requireCapitalization Whether a match must start with an uppercase letter.
     * @param classification A classification label for the type of information.
     */
    public FuzzyDictionaryFilter(final FilterType filterType, final FilterConfiguration filterConfiguration,
                                 final SensitivityLevel sensitivityLevel, final Set<String> terms, final boolean requireCapitalization,
                                 final String classification) {
        super(filterType, filterConfiguration);

        this.sensitivityLevel = sensitivityLevel;
        this.dictionary = loadData(terms);
        this.maxNgrams = getMaxNgrams();
        this.requireCapitalization = requireCapitalization;
        this.classification = classification;

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, final String context, int piece, String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        if(policy.getIdentifiers().hasFilter(filterType)) {

            // Build ngrams from the input text.
            final Map<Integer, Map<Position, String>> ngrams = new HashMap<>();

            for(int x = 1; x <= maxNgrams; x++) {
                ngrams.put(x, getNgramsOfLength(input, x));
            }

            for(final String entry : dictionary.keySet()) {

                // Exact matches: every occurrence, not only the first. Each is remembered, whether or not
                // the capitalization check reports it, so the fuzzy pass does not report it again.
                final List<Position> exactMatches = new ArrayList<>();
                final Matcher matcher = dictionary.get(entry).matcher(input);

                while (matcher.find()) {

                    final int startPosition = matcher.start();

                    // The match is case-insensitive and the term is interpolated into the
                    // pattern as-is, so take the end and text from the matcher, not the term.
                    final int endPosition = matcher.end();
                    final String matched = matcher.group();

                    exactMatches.add(new Position(startPosition, endPosition));

                    if (!requireCapitalization || Character.isUpperCase(input.charAt(startPosition))) {
                        spans.add(createSpan(contextService, input, startPosition, endPosition, 1.0, context, matched, policy));
                    }

                }

                // Fuzzy matches, whether or not the term also appears exactly, so a misspelling is found
                // alongside a correct spelling. Only when the sensitivity level is not "off".
                if(sensitivityLevel != SensitivityLevel.OFF) {

                    final int wordsInEntry = wordCount(entry);
                    final String normalizedEntry = normalizeWhitespace(entry);
                    final boolean trimLeading = !normalizedEntry.isEmpty() && Character.isLetterOrDigit(normalizedEntry.charAt(0));
                    final boolean trimTrailing = !normalizedEntry.isEmpty()
                            && Character.isLetterOrDigit(normalizedEntry.charAt(normalizedEntry.length() - 1));

                    if (ngrams.containsKey(wordsInEntry)) {
                        for (final Position position : ngrams.get(wordsInEntry).keySet()) {

                            // N-grams are split on whitespace, so punctuation next to a word is part of
                            // it: "Springfeld," would be two edits from "Springfield". Compare and report
                            // the word without it, as the exact dictionary filter does, unless the term
                            // itself has punctuation at that end.
                            final String token = ngrams.get(wordsInEntry).get(position);
                            int begin = 0;
                            int finish = token.length();
                            while (trimLeading && begin < finish && !Character.isLetterOrDigit(token.charAt(begin))) {
                                begin++;
                            }
                            while (trimTrailing && finish > begin && !Character.isLetterOrDigit(token.charAt(finish - 1))) {
                                finish--;
                            }

                            final int start = position.getStart() + begin;
                            final int end = position.getStart() + finish;

                            if (overlapsAny(exactMatches, start, end)) {
                                continue;
                            }

                            // Compare string distance between word and ngrams.
                            final String ngram = token.substring(begin, finish);

                            if (ngram.length() > 2 && sensitivityLevel == SensitivityLevel.AUTO) {

                                // AUTO allows more edits for longer words, counts two swapped neighboring
                                // letters as one edit, and requires a capital letter, so that common short
                                // words ("and", "the") do not match names and places in a large list.
                                final String word = normalizeWhitespace(ngram).toLowerCase();
                                final String term = normalizedEntry.toLowerCase();
                                final int allowed = autoEditsAllowed(word.length());

                                if (Character.isUpperCase(ngram.charAt(0)) && Math.abs(term.length() - word.length()) <= allowed) {
                                    final int distance = optimalStringAlignmentDistance(term, word);
                                    if (distance <= allowed) {
                                        spans.add(createSpan(contextService, input, start, end, confidenceFor(distance), context, ngram, policy));
                                    }
                                }

                            } else if (ngram.length() > 2) {

                                // TODO: Should this be customizable in the dictionary's properties in the filter policy?
                                final LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
                                final int distance = levenshteinDistance.apply(normalizedEntry.toLowerCase(), normalizeWhitespace(ngram).toLowerCase());

                                if (!requireCapitalization || Character.isUpperCase(ngram.charAt(0))) {

                                    // Higher sensitivity requires a closer match: HIGH matches only
                                    // exact tokens, MEDIUM allows a Levenshtein distance up to 1, and
                                    // LOW allows a distance up to 2. The thresholds are distinct so the
                                    // three levels behave differently (previously MEDIUM and LOW were
                                    // both an effective distance of 2 and so were indistinguishable).
                                    if (sensitivityLevel == SensitivityLevel.HIGH && distance == 0) {
                                        spans.add(createSpan(contextService, input, start, end, 0.9, context, ngram, policy));
                                    } else if (sensitivityLevel == SensitivityLevel.MEDIUM && distance <= 1) {
                                        spans.add(createSpan(contextService, input, start, end, 0.7, context, ngram, policy));
                                    } else if (sensitivityLevel == SensitivityLevel.LOW && distance <= 2) {
                                        spans.add(createSpan(contextService, input, start, end, 0.5, context, ngram, policy));
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

    /**
     * The edits {@link SensitivityLevel#AUTO} allows for a word of this length: none under five
     * characters, one under nine, and two otherwise.
     */
    public static int autoEditsAllowed(final int length) {
        if (length < 5) {
            return 0;
        }
        return length < 9 ? 1 : 2;
    }

    /** The confidence of a fuzzy match at this distance, as the fixed levels assign it. */
    private static double confidenceFor(final int distance) {
        if (distance == 0) {
            return 0.9;
        }
        return distance == 1 ? 0.7 : 0.5;
    }

    /**
     * The optimal string alignment distance: the Levenshtein distance, except that swapping two
     * neighboring characters counts as one edit rather than two, so "smiht" is one edit from "smith".
     */
    public static int optimalStringAlignmentDistance(final String a, final String b) {
        final int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                final int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[a.length()][b.length()];
    }

    /** Whether {@code [start, end)} overlaps any of the positions. */
    private static boolean overlapsAny(final List<Position> positions, final int start, final int end) {
        for (final Position position : positions) {
            if (start < position.getEnd() && position.getStart() < end) {
                return true;
            }
        }
        return false;
    }

    private Span createSpan(ContextService contextService, String text, int characterStart, int characterEnd, double confidence, String context,
                            String token, Policy policy) throws Exception {

        final boolean ignored = isIgnored(token);
        final String[] window = getWindow(text, characterStart, characterEnd);

        // Get the replacement token or the original token if no filter strategy conditions are met.
        final Replacement replacement = getReplacement(contextService, policy, context, token,
                window, confidence, classification, null);

        // Add the span to the list.
        final Span span = Span.make(characterStart, characterEnd, getFilterType(), context,
                confidence, token, replacement.getReplacement(),
                replacement.getSalt(), ignored, replacement.isApplied(), window, priority);

        span.setColor(replacement.getColor());
        span.setClassification(classification);

        return span;

    }

    private int getMaxNgrams() {

        // Get the max number of n-grams to break the text up into based on the
        // max number of spaces in any individual entry in the dictionary, up to
        // a max of 20.
        // TODO: Externalize the limit of 20.

        int maxNgrams = 0;

        for(final String key : dictionary.keySet()) {
            final int n = wordCount(key);
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
            dictionary.put(term, termPattern(term));
        }

        return dictionary;

    }

}

