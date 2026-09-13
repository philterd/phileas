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
package ai.philterd.phileas.services;

import ai.philterd.phileas.filters.rules.RegexProbe;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.IgnoredPattern;
import ai.philterd.phileas.policy.InvalidPolicyPatternException;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.AbstractFilter;
import ai.philterd.phileas.policy.filters.Identifier;
import ai.philterd.phileas.policy.filters.Section;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Compiles and probes every regular expression a policy supplies. A policy-supplied pattern is
 * otherwise only exercised when a document reaches it, so a bad pattern surfaces as a failure of
 * whichever document happens to produce a matching token rather than as a failure of the policy.
 */
final class PolicyPatternValidator {

    private final long regexTimeoutMs;

    PolicyPatternValidator(final long regexTimeoutMs) {
        this.regexTimeoutMs = regexTimeoutMs;
    }

    /**
     * Validates every pattern in a policy.
     * @param policy The {@link Policy} to validate.
     * @throws InvalidPolicyPatternException if a pattern does not compile or fails its probe.
     */
    void validate(final Policy policy) {

        final Identifiers identifiers = policy.getIdentifiers();

        if (identifiers != null) {

            for (final Identifier identifier : orEmpty(identifiers.getIdentifiers())) {
                final int flags = identifier.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
                check("identifier '" + identifier.getClassification() + "' pattern",
                        identifier.getPattern(), flags);
            }

            for (final Section section : orEmpty(identifiers.getSections())) {
                // Compiled separately so a syntax error names the offending half, then probed as the
                // joined pattern the filter actually runs.
                compile("section start pattern", section.getStartPattern(), 0);
                compile("section end pattern", section.getEndPattern(), 0);
                if (section.getStartPattern() != null && section.getEndPattern() != null) {
                    check("section pattern",
                            "(?:" + section.getStartPattern() + ")(.*?)(?:" + section.getEndPattern() + ")", 0);
                }
            }

            for (final AbstractFilter filter : identifiers.getAllFilters()) {
                checkIgnoredPatterns(filter.getIgnoredPatterns(),
                        "the " + filter.getClass().getSimpleName() + " filter");
            }

        }

        checkIgnoredPatterns(policy.getIgnoredPatterns(), "the policy");

    }

    private void checkIgnoredPatterns(final List<IgnoredPattern> ignoredPatterns, final String owner) {

        for (final IgnoredPattern ignoredPattern : orEmpty(ignoredPatterns)) {
            check("ignored pattern " + name(ignoredPattern) + "on " + owner, ignoredPattern.getPattern(), 0);
        }

    }

    private void check(final String description, final String regex, final int flags) {

        final Pattern pattern = compile(description, regex, flags);

        // With the budget disabled there is no clock to stop a probe that does not finish, so the
        // pattern is compiled but not run. Patterns are still guarded the same way at match time.
        if (pattern != null && regexTimeoutMs > 0) {
            RegexProbe.probe(pattern, regexTimeoutMs).ifPresent(failure -> {
                throw new InvalidPolicyPatternException(String.format(
                        "The %s %s. Pattern: %s", description, failure, regex));
            });
        }

    }

    private Pattern compile(final String description, final String regex, final int flags) {

        if (regex == null) {
            return null;
        }

        try {
            return Pattern.compile(regex, flags);
        } catch (final PatternSyntaxException e) {
            throw new InvalidPolicyPatternException(String.format(
                    "The %s is not a valid regular expression: %s. Pattern: %s",
                    description, e.getDescription(), regex), e);
        }

    }

    private static String name(final IgnoredPattern ignoredPattern) {
        return ignoredPattern.getName() == null ? "" : "'" + ignoredPattern.getName() + "' ";
    }

    private static <T> List<T> orEmpty(final List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

}
