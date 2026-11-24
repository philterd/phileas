/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.filters.regex;

import ai.philterd.phileas.model.filtering.Cue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cue words that a value must follow to count, such as {@code Member ID} before a member ID. A cue word
 * matches case-insensitively as a whole word, a multi-word cue word as a phrase, and may be followed by
 * punctuation such as {@code :} or {@code #}. It must lie within the given number of tokens before the
 * value.
 */
public final class CueWords {

    // A token is a run of non-whitespace, except that '#' starts a new one, so "Member#:" is "Member" and
    // "#:", which matches the cue word "member #".
    private static final Pattern TOKEN = Pattern.compile("[^\\s#]+|#\\S*");

    private final List<String[]> cueWords;

    /**
     * @param defaults The filter's default cue words.
     * @param additional Cue words from the policy, or null.
     * @param useDefaults Whether the default cue words are used along with the additional ones.
     */
    public CueWords(final Collection<String> defaults, final Collection<String> additional, final boolean useDefaults) {

        final Set<String> all = new LinkedHashSet<>();

        if(useDefaults) {
            all.addAll(defaults);
        }

        if(additional != null) {
            all.addAll(additional);
        }

        this.cueWords = new ArrayList<>();

        for(final String cueWord : all) {
            final String[] tokens = tokens(cueWord).toArray(new String[0]);
            if(tokens.length > 0) {
                // Punctuation ending the cue word is optional in the text, so "serial no." also matches
                // "Serial No:" and "Serial No". A cue word that is only punctuation, such as "#", keeps it.
                tokens[tokens.length - 1] = stripTrailingPunctuation(tokens[tokens.length - 1]);
                cueWords.add(tokens);
            }
        }

    }

    private record Token(String text, int start, int end) { }

    /**
     * Whether a cue word lies within the given number of tokens before a position in the text.
     * @param input The text.
     * @param position The start of the value.
     * @param windowSize The number of tokens before the value to look in.
     * @return {@code true} if a cue word is there.
     */
    public boolean precede(final String input, final int position, final int windowSize) {
        return nearest(input, position, windowSize) != null;
    }

    /**
     * The cue word nearest before a position in the text, within the given number of tokens.
     * @param input The text.
     * @param position The start of the value.
     * @param windowSize The number of tokens before the value to look in.
     * @return The nearest cue word, or null if none is there.
     */
    public Cue nearest(final String input, final int position, final int windowSize) {

        final List<Token> before = lastTokens(input, position, windowSize);

        Cue nearest = null;

        for(final String[] cueWord : cueWords) {
            for(int start = 0; start + cueWord.length <= before.size(); start++) {
                if(matchesAt(before, start, cueWord)) {
                    final Cue cue = new Cue(before.get(start).start(), before.get(start + cueWord.length - 1).end());
                    if(nearest == null || cue.isNearerThan(nearest)) {
                        nearest = cue;
                    }
                }
            }
        }

        return nearest;

    }

    private static boolean matchesAt(final List<Token> tokens, final int start, final String[] cueWord) {

        for(int i = 0; i < cueWord.length; i++) {

            String token = tokens.get(start + i).text();

            // Opening punctuation, as in "(Member", may come before the cue word.
            if(i == 0) {
                token = stripLeadingPunctuation(token);
            }

            final boolean last = i == cueWord.length - 1;

            if(last) {
                // The last word may be followed by punctuation, but not by a letter or digit, so "SN"
                // matches "SN:" and not "SNOW".
                if(!token.startsWith(cueWord[i]) || !isPunctuation(token.substring(cueWord[i].length()))) {
                    return false;
                }
            } else if(!token.equals(cueWord[i])) {
                return false;
            }

        }

        return true;

    }

    /** The last tokens before the position, at most windowSize of them, lower-cased, with their offsets. */
    private static List<Token> lastTokens(final String input, final int position, final int windowSize) {

        // Enough text to hold windowSize tokens: walk back over that many runs of non-whitespace.
        int start = position;
        int seen = 0;
        while(start > 0 && seen <= windowSize) {
            start--;
            if(Character.isWhitespace(input.charAt(start)) && start + 1 < position
                    && !Character.isWhitespace(input.charAt(start + 1))) {
                seen++;
            }
        }

        final List<Token> tokens = tokens(input.substring(start, position), start);

        return tokens.size() <= windowSize ? tokens : tokens.subList(tokens.size() - windowSize, tokens.size());

    }

    private static List<Token> tokens(final String text, final int offset) {

        final List<Token> tokens = new ArrayList<>();
        // Each token is lower-cased on its own, since lower-casing can change the text's length.
        final Matcher matcher = TOKEN.matcher(text);

        while(matcher.find()) {
            tokens.add(new Token(matcher.group().toLowerCase(Locale.ROOT), offset + matcher.start(), offset + matcher.end()));
        }

        return tokens;

    }

    private static List<String> tokens(final String text) {
        return tokens(text, 0).stream().map(Token::text).toList();
    }

    private static String stripLeadingPunctuation(final String token) {

        int i = 0;
        while(i < token.length() && !Character.isLetterOrDigit(token.charAt(i)) && token.charAt(i) != '#') {
            i++;
        }

        return token.substring(i);

    }

    private static String stripTrailingPunctuation(final String token) {

        int end = token.length();
        while(end > 0 && !Character.isLetterOrDigit(token.charAt(end - 1))) {
            end--;
        }

        return end == 0 ? token : token.substring(0, end);

    }

    private static boolean isPunctuation(final String text) {
        return text.chars().noneMatch(Character::isLetterOrDigit);
    }

    @Override
    public String toString() {
        return cueWords.stream().map(words -> String.join(" ", Arrays.asList(words))).toList().toString();
    }

}
