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

import ai.philterd.phileas.model.filtering.FilterType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * City names in the bundled list that are also common English words, such as {@code Call}, {@code Hope}, and
 * {@code Phoenix}. One of these is taken as a city only in a location context: it begins with a capital letter,
 * and either follows a location word ({@code in Phoenix}, {@code from Hope}) or is followed by a comma and a
 * state ({@code Mobile, Alabama}, {@code Reading, PA}).
 */
public final class CityCommonWords {

    private static final Set<String> LOCATION_WORDS = Set.of("in", "from", "to", "near", "outside", "toward", "towards", "via");

    private static final Set<String> STATE_ABBREVIATIONS = Set.of("AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC",
            "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT",
            "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT",
            "VT", "VA", "WA", "WV", "WI", "WY");

    private static final Set<String> WORDS = load();

    private CityCommonWords() {
    }

    /** Whether a city name, in any case, is also a common word. */
    public static boolean isCommonWord(final String city) {
        return WORDS.contains(city.toLowerCase(Locale.ROOT));
    }

    /**
     * Whether the text at {@code [start, end)} is in a location context.
     * @param text The text.
     * @param start The match's start.
     * @param end The match's end.
     * @return {@code true} if the match begins with a capital letter and follows a location word or is
     * followed by a comma and a state name or abbreviation.
     */
    public static boolean inLocationContext(final String text, final int start, final int end) {

        if (start >= end || !Character.isUpperCase(text.charAt(start))) {
            return false;
        }

        return LOCATION_WORDS.contains(previousWord(text, start).toLowerCase(Locale.ROOT)) || followedByState(text, end);

    }

    /** The letters of the word before {@code start}, skipping whitespace and an opening parenthesis or quote. */
    private static String previousWord(final String text, final int start) {

        int end = start;
        while (end > 0 && (Character.isWhitespace(text.charAt(end - 1)) || "(\"'".indexOf(text.charAt(end - 1)) >= 0)) {
            end--;
        }

        int begin = end;
        while (begin > 0 && Character.isLetter(text.charAt(begin - 1))) {
            begin--;
        }

        return text.substring(begin, end);

    }

    private static boolean followedByState(final String text, final int end) {

        if (end >= text.length() || text.charAt(end) != ',') {
            return false;
        }

        int begin = end + 1;
        while (begin < text.length() && Character.isWhitespace(text.charAt(begin))) {
            begin++;
        }

        final String rest = text.substring(begin);

        // An abbreviation is two capitals ending the word: "PA", "PA.", but not "PAID".
        if (rest.length() >= 2 && STATE_ABBREVIATIONS.contains(rest.substring(0, 2))
                && (rest.length() == 2 || !Character.isLetter(rest.charAt(2)))) {
            return true;
        }

        try {
            for (final String state : DictionaryFilter.getPredefinedDictionary(FilterType.LOCATION_STATE).lowerCaseTerms()) {
                if (rest.regionMatches(true, 0, state, 0, state.length())
                        && (rest.length() == state.length() || !Character.isLetter(rest.charAt(state.length())))) {
                    return true;
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return false;

    }

    private static Set<String> load() {

        final Set<String> words = new HashSet<>();

        try (InputStream inputStream = CityCommonWords.class.getClassLoader().getResourceAsStream("city-common-words.txt");
             final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank() && !line.startsWith("#")) {
                    words.add(line.trim().toLowerCase(Locale.ROOT));
                }
            }

        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return Set.copyOf(words);

    }

}
