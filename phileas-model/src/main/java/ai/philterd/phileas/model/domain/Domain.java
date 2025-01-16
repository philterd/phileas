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
package ai.philterd.phileas.model.domain;

import ai.philterd.phileas.model.policy.Ignored;
import ai.philterd.phileas.model.policy.filters.AbstractFilter;

import java.util.List;

/**
 * Represents an industry domain in which Philter can be used.
 * A domain provides convenience policy configuration
 * options.
 */
public abstract class Domain {

    // These are the domain names given in the policy
    // used to trigger the activation of the respective domain.
    public static final String DOMAIN_LEGAL = "legal";
    public static final String DOMAIN_HEALTH = "health";

    /**
     * The terms that should be ignored in the domain.
     * These terms will be ignored globally.
     * @return A list of {@link Ignored} terms.
     */
    public abstract Ignored getIgnored();

    /**
     * A list of {@link AbstractFilter} that should be used for the
     * domain.
     *
     * These filters will only be applied if the same
     * filter is not already defined in the policy. By enabling
     * a domain and then still adding the filter to the policy
     * the user can override this filter (can disable it, customize it, etc.)
     * @return A list of {@link AbstractFilter}.
     */
    public abstract List<AbstractFilter> getFilters();

}
