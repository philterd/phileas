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
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import org.apache.hc.client5.http.classic.HttpClient;

import java.security.SecureRandom;
import java.util.List;

/** Base for filter services that redact text documents. */
public abstract class TextFilterService extends FilterService {

    /**
     * Filter text using the context and vector services bound at construction.
     * @param policy The {@link Policy} to apply.
     * @param context The redaction context.
     * @param input The input document.
     * @return A {@link TextFilterResult}.
     * @throws Exception Thrown if the text cannot be filtered.
     */
    public abstract TextFilterResult filter(final Policy policy, final String context, final String input) throws Exception;

    /**
     * Filter text with a per-call {@link ContextService} and {@link VectorService}.
     * @param policy The {@link Policy} to apply.
     * @param contextService The {@link ContextService} for this request.
     * @param vectorService The {@link VectorService} for this request.
     * @param context The redaction context.
     * @param input The input document.
     * @return A {@link TextFilterResult}.
     * @throws Exception Thrown if the text cannot be filtered.
     */
    public abstract TextFilterResult filter(final Policy policy, final ContextService contextService,
                                            final VectorService vectorService, final String context,
                                            final String input) throws Exception;

    /**
     * Redact a list of spans in a text document.
     * @param input The input document as a byte array.
     * @param spans A list of {@link Span spans}.
     * @return A byte array containing the filtered document.
     */
    public abstract byte[] apply(final byte[] input, final List<Span> spans);

    protected TextFilterService(final PhileasConfiguration phileasConfiguration,
                                final SecureRandom random,
                                final HttpClient httpClient) {

        super(phileasConfiguration, random, httpClient);

    }

}
