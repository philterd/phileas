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
package ai.philterd.phileas.filters.rules.regex;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.RulesFilter;
import ai.philterd.phileas.services.Analyzer;

/**
 * A filter that works by using one or more regular expressions.
 */
public abstract class RegexFilter extends RulesFilter {

    protected Analyzer analyzer;

    /**
     * Creates a new regular expression-based filter.
     * @param filterType
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     */
    public RegexFilter(final FilterType filterType, final FilterConfiguration filterConfiguration) {
        super(filterType, filterConfiguration);
    }

}
