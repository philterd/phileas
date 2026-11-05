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
package ai.philterd.phileas.policy;

import ai.philterd.phileas.filters.rules.DeadlineCharSequence;
import ai.philterd.phileas.filters.rules.RegexTimeoutException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

public class IgnoredPattern {

    private static final Logger LOGGER = LogManager.getLogger(IgnoredPattern.class);

    /** The regex time budget used when none is given, the default of {@code regex.timeout.ms}. */
    public static final long DEFAULT_BUDGET_MS = 1000;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("pattern")
    @Expose
    private String pattern;

    // The compiled form of the pattern, cached so a Pattern is not compiled on every match. It is
    // built lazily (Gson sets the pattern field directly, bypassing the setter) and invalidated
    // whenever the pattern changes.
    private transient Pattern compiledPattern;

    public IgnoredPattern() {

    }

    public IgnoredPattern(String name, String pattern) {
        this.name = name;
        this.pattern = pattern;
    }

    public IgnoredPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return pattern;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.name)
                .append(this.pattern)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof IgnoredPattern) {

            final IgnoredPattern ip = (IgnoredPattern) obj;

            return (StringUtils.equals(ip.name, this.name) && StringUtils.equals(ip.pattern, this.pattern));

        }

        return false;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.compiledPattern = null;
    }

    /**
     * Determines whether the input matches this ignored pattern, bounded by the default regex time budget
     * ({@value #DEFAULT_BUDGET_MS} ms). See {@link #matches(String, long)}.
     * @param input The text to test.
     * @return <code>true</code> if the input fully matches the pattern.
     */
    public boolean matches(final String input) {
        return matches(input, DEFAULT_BUDGET_MS);
    }

    /**
     * Determines whether the input matches this ignored pattern, using a cached compiled
     * {@link Pattern} so the regex is compiled at most once per pattern value. The match is bounded by
     * {@code budgetMs}, as a filter's own patterns are by {@code regex.timeout.ms}, so a pattern that
     * backtracks catastrophically cannot hang the thread. A match that runs out of time counts as not
     * matching, so the input is not ignored and stays redacted.
     * @param input The text to test.
     * @param budgetMs The time a match may take, in milliseconds. Zero or less disables the bound.
     * @return <code>true</code> if the input fully matches the pattern within the budget.
     */
    public boolean matches(final String input, final long budgetMs) {
        Pattern p = compiledPattern;
        if (p == null) {
            p = Pattern.compile(pattern);
            compiledPattern = p;
        }
        final DeadlineCharSequence guarded = new DeadlineCharSequence(input, budgetMs);
        guarded.startClock();
        try {
            return p.matcher(guarded).matches();
        } catch (final RegexTimeoutException e) {
            // The token itself is not logged: it is the sensitive value being tested.
            LOGGER.warn("Ignored pattern took longer than {} ms on a {} character token, so the token is not "
                    + "ignored. Pattern: {}", budgetMs, input.length(), pattern);
            return false;
        } catch (final StackOverflowError e) {
            // A non-atomic repeated group recurses per character, so a long token overflows the
            // stack. Caught only to name the pattern in the failure. See issue #357.
            throw new RegexMatchFailedException(String.format(
                    "Ignored pattern overflowed the stack on a %d character token. Pattern: %s",
                    input.length(), pattern));
        }
    }

}
