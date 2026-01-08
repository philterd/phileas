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
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.ContextService;
import org.apache.hc.client5.http.classic.HttpClient;

import java.util.List;
import java.util.Random;

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

    /**
     * Redact a list of spans in a text document.
     * @param input The input document as a byte array.
     * @param spans A list of {@link Span spans}.
     * @return A byte array containing the filtered document.
     */
    public abstract byte[] apply(final byte[] input, final List<Span> spans);

    protected TextFilterService(final PhileasConfiguration phileasConfiguration,
                                final ContextService contextService,
                                final Random random,
                                final HttpClient httpClient) {

        super(phileasConfiguration, contextService, random, httpClient);

    }

}
