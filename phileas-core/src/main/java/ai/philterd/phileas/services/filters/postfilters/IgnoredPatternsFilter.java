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
package ai.philterd.phileas.services.filters.postfilters;

import ai.philterd.phileas.model.filtering.PostFiltered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.IgnoredPattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Implementation of {@link PostFilter} that removes tokens matching a given pattern.
 */
public class IgnoredPatternsFilter extends PostFilter {

    private static final Logger LOGGER = LogManager.getLogger(IgnoredPatternsFilter.class);

    private final List<IgnoredPattern> ignoredPatterns;
    private final long regexTimeoutMs;

    public IgnoredPatternsFilter(final List<IgnoredPattern> ignoredPatterns) {
        this(ignoredPatterns, IgnoredPattern.DEFAULT_BUDGET_MS);
    }

    /**
     * @param ignoredPatterns The patterns that ignore a span in the whole document.
     * @param regexTimeoutMs The time a match may take, as for a filter's own patterns ({@code regex.timeout.ms}).
     */
    public IgnoredPatternsFilter(final List<IgnoredPattern> ignoredPatterns, final long regexTimeoutMs) {

        this.ignoredPatterns = ignoredPatterns;
        this.regexTimeoutMs = regexTimeoutMs;

    }

    @Override
    protected PostFiltered process(final String text, final Span span) {

        final String spanText = span.getText(text);

        for(final IgnoredPattern pattern : ignoredPatterns) {

            final boolean ignored = pattern.matches(spanText, regexTimeoutMs);

            if(ignored) {
                return new PostFiltered(span, true);
            }

        }

        return new PostFiltered(span, false);

    }

}
