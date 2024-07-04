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
package ai.philterd.phileas.services.postfilters;

import ai.philterd.phileas.model.objects.PostFilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.IgnoredPattern;
import ai.philterd.phileas.model.services.PostFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Implementation of {@link PostFilter} that removes tokens matching a given pattern.
 */
public class IgnoredPatternsFilter extends PostFilter {

    private static final Logger LOGGER = LogManager.getLogger(IgnoredPatternsFilter.class);

    private final List<IgnoredPattern> ignoredPatterns;

    public IgnoredPatternsFilter(final List<IgnoredPattern> ignoredPatterns) {

        this.ignoredPatterns = ignoredPatterns;

    }

    @Override
    protected PostFilterResult process(final String text, final Span span) {

        final String spanText = span.getText(text);

        for(final IgnoredPattern pattern : ignoredPatterns) {

            final boolean ignored = spanText.matches(pattern.getPattern());

            if(ignored) {
                return new PostFilterResult(span, true);
            }

        }

        return new PostFilterResult(span, false);

    }

}
