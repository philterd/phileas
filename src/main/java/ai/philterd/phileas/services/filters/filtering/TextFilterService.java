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
package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.ContextService;

public abstract class TextFilterService extends FilterService {

    /**
     * Filter text from plain text.
     * @param policy The {@link Policy} to apply.
     * @param context The redaction context.
     * @param input The input document as a byte array.
     * @return A {@link TextFilterResult}.
     * @throws Exception Thrown if the text cannot be filtered.
     */
    public abstract TextFilterResult filter(final Policy policy, final String context, final String input) throws Exception;

    protected TextFilterService(final PhileasConfiguration phileasConfiguration,
                               final ContextService contextService) {

        super(phileasConfiguration, contextService);

    }

}
