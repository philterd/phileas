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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@link Domain} for legal.
 */
public class LegalDomain extends Domain {

    private static LegalDomain legalDomain;

    /**
     * Get an instance of the {@link LegalDomain} class.
     * @return An instance of the {@link LegalDomain} class.
     */
    public static Domain getInstance() {

        if(legalDomain == null) {
            legalDomain = new LegalDomain();
        }

        return legalDomain;

    }

    @Override
    public Ignored getIgnored() {

        final Ignored ignored = new Ignored();

        ignored.setName("legal-domain");
        ignored.setTerms(Arrays.asList("Defendant", "Plaintiff", "Court"));

        return ignored;

    }

    @Override
    public List<AbstractFilter> getFilters() {

        final List<AbstractFilter> filters = new LinkedList<>();

        // TODO: Determine what filters are appropriate.

        return filters;

    }

}
