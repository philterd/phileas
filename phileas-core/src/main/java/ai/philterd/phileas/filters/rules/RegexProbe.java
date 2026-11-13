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
package ai.philterd.phileas.filters.rules;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs a compiled pattern against canary inputs under a time budget so a pattern that would exhaust
 * the budget or overflow the stack on a document can be rejected when the policy is loaded instead.
 * A pattern is run the way it is applied while filtering, since that changes its cost: one beginning
 * with {@code .*} is quadratic when searched for but linear when matched against the whole text.
 * This detects, it does not prove: a pattern that misbehaves only on input unlike any canary passes.
 */
public final class RegexProbe {

    /**
     * How a pattern is applied to text while filtering, and so how the probe runs it.
     */
    public enum Application {

        /**
         * Searched for anywhere in a document with {@link Matcher#find()}, as a filter's own patterns and
         * the identifier and section patterns a policy supplies are.
         */
        FIND,

        /**
         * Anchored to the whole of a span's text with {@link Matcher#matches()}, as an ignored pattern is.
         */
        MATCHES

    }

    // A recursive pattern needs a few thousand characters to overflow.
    private static final int CANARY_LENGTH = 16 * 1024;

    private static final List<String> BASE_UNITS = List.of("a", "A", "0", " ", "a0 ");

    private static final int MAX_UNITS = 12;

    private RegexProbe() {

    }

    /**
     * Probes a pattern that is searched for with {@link Matcher#find()}. See
     * {@link #probe(Pattern, long, Application)}.
     * @param pattern The compiled {@link Pattern} to probe.
     * @param budgetMs The time budget for one canary, in milliseconds. Zero or less runs unguarded.
     * @return A description of how the pattern failed, or empty when every canary completed.
     */
    public static Optional<String> probe(final Pattern pattern, final long budgetMs) {
        return probe(pattern, budgetMs, Application.FIND);
    }

    /**
     * Probes a pattern with each canary in turn, applying it as the {@link Application} says.
     * @param pattern The compiled {@link Pattern} to probe.
     * @param budgetMs The time budget for one canary, in milliseconds. Zero or less runs unguarded.
     * @param application How the pattern is applied to text while filtering.
     * @return A description of how the pattern failed, or empty when every canary completed.
     */
    public static Optional<String> probe(final Pattern pattern, final long budgetMs, final Application application) {

        for (final String unit : units(pattern.pattern())) {

            final String canary = unit.repeat(CANARY_LENGTH / unit.length());

            final DeadlineCharSequence guarded = new DeadlineCharSequence(canary, budgetMs);
            final Matcher matcher = pattern.matcher(guarded);
            guarded.startClock();

            try {
                if (application == Application.MATCHES) {
                    matcher.matches();
                } else {
                    matcher.find();
                }
            } catch (final RegexTimeoutException e) {
                return Optional.of(String.format("exceeded the %d ms budget on %d characters of \"%s\"",
                        budgetMs, canary.length(), unit));
            } catch (final StackOverflowError e) {
                return Optional.of(String.format("overflowed the stack on %d characters of \"%s\"",
                        canary.length(), unit));
            }

        }

        return Optional.empty();

    }

    /**
     * The repeating units the canaries are built from: a fixed set covering the common alphabets,
     * plus the pattern's own literal characters, since a run of one character exercises only a
     * pattern that accepts that character.
     */
    private static List<String> units(final String regex) {

        final Set<String> units = new LinkedHashSet<>(BASE_UNITS);

        int i = 0;

        while (i < regex.length() && units.size() < MAX_UNITS) {

            final char c = regex.charAt(i);

            if (c == '\\') {
                // Skip the escaped character: the 'd' of "\d" is not a literal.
                i += 2;
            } else {
                if (Character.isLetterOrDigit(c)) {
                    units.add(String.valueOf(c));
                }
                i++;
            }

        }

        return List.copyOf(units);

    }

}
