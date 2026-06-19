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
import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationServiceFactory;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.documentprocessors.DocumentProcessor;
import ai.philterd.phileas.services.documentprocessors.UnstructuredDocumentProcessor;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.split.SplitFactory;
import ai.philterd.phileas.services.split.SplitService;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Implementation of {@link FilterService} that filters plain text.
 */
public class PlainTextFilterService extends TextFilterService {

	private static final Logger LOGGER = LogManager.getLogger(PlainTextFilterService.class);

    private final DocumentProcessor unstructuredDocumentProcessor;

    public PlainTextFilterService(final PhileasConfiguration phileasConfiguration,
                                  final ContextService contextService,
                                  final VectorService vectorService,
                                  final HttpClient httpClient) {

        this(phileasConfiguration, contextService, vectorService, new SecureRandom(), httpClient);

    }

    public PlainTextFilterService(final PhileasConfiguration phileasConfiguration,
                                  final ContextService contextService,
                                  final VectorService vectorService,
                                  final Random random,
                                  final HttpClient httpClient) {

        super(phileasConfiguration, contextService, random, httpClient);

        LOGGER.info("Initializing plain text filter service.");

        // Create a new unstructured document processor.
        this.unstructuredDocumentProcessor = new UnstructuredDocumentProcessor(
                SpanDisambiguationServiceFactory.getSpanDisambiguationService(phileasConfiguration, vectorService),
                phileasConfiguration.incrementalRedactionsEnabled()
        );

    }

    @Override
    public TextFilterResult filter(final Policy policy, final String context, final String input) throws Exception {

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
        final List<PostFilter> postFilters = getPostFiltersForPolicy(policy);

        return filter(policy, filters, postFilters, context, input);

    }

    /**
     * Resolves a policy's filters and post-filters once and returns a reusable handle, so per-row
     * callers (for example a Spark or Kafka UDF) do not re-resolve the policy on every call. The
     * returned {@link PreparedPolicy} produces the same result as {@link #filter(Policy, String,
     * String)} with this policy, and is safe to reuse across calls and across threads.
     * @param policy The {@link Policy} to prepare.
     * @return A {@link PreparedPolicy} bound to the resolved filters for {@code policy}.
     * @throws Exception Thrown if the policy's filters cannot be built.
     */
    public PreparedPolicy prepare(final Policy policy) throws Exception {
        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
        final List<PostFilter> postFilters = getPostFiltersForPolicy(policy);
        return new PreparedPolicy(policy, filters, postFilters);
    }

    // Shared processing path used by both filter(policy, context, input) and a PreparedPolicy, after
    // the policy's filters and post-filters have been resolved.
    private TextFilterResult filter(final Policy policy, final List<Filter> filters,
                                    final List<PostFilter> postFilters, final String context,
                                    final String input) throws Exception {

        final TextFilterResult textFilterResult;

        // Do we need to split the input text due to its size?
        if (policy.getConfig().getSplitting().isEnabled() && input.length() >= policy.getConfig().getSplitting().getThreshold()) {

            // Get the splitter to use from the policy.
            final SplitService splitService = SplitFactory.getSplitService(
                    policy.getConfig().getSplitting().getMethod(),
                    policy.getConfig().getSplitting().getThreshold()
            );

            // Holds all filter responses that will ultimately be combined into a single response.
            final List<TextFilterResult> filterResponse = new LinkedList<>();

            // Split the string.
            final List<String> splits = splitService.split(input);

            // Process each split.
            for (int i = 0; i < splits.size(); i++) {
                final TextFilterResult fr = unstructuredDocumentProcessor.process(policy, filters, postFilters, context, i, splits.get(i));
                filterResponse.add(fr);
            }

            // Combine the results into a single filterResponse object.
            textFilterResult = TextFilterResult.combine(filterResponse, context, splitService.getSeparator());

        } else {

            // Do not split. Process the entire string at once.
            textFilterResult = unstructuredDocumentProcessor.process(policy, filters, postFilters, context, 0, input);

        }

        return textFilterResult;

    }

    /**
     * A policy with its filters and post-filters already resolved, for repeated filtering without the
     * per-call policy resolution. Create one with {@link PlainTextFilterService#prepare(Policy)} and
     * reuse it. Like the service itself, it is safe to call concurrently on a shared instance.
     */
    public final class PreparedPolicy {

        private final Policy policy;
        private final List<Filter> filters;
        private final List<PostFilter> postFilters;

        private PreparedPolicy(final Policy policy, final List<Filter> filters, final List<PostFilter> postFilters) {
            this.policy = policy;
            this.filters = filters;
            this.postFilters = postFilters;
        }

        /**
         * Filters text using this prepared policy. Equivalent to {@link
         * PlainTextFilterService#filter(Policy, String, String)} but without re-resolving the policy.
         * @param context The redaction context.
         * @param input The input text.
         * @return A {@link TextFilterResult}.
         * @throws Exception Thrown if the text cannot be filtered.
         */
        public TextFilterResult filter(final String context, final String input) throws Exception {
            return PlainTextFilterService.this.filter(policy, filters, postFilters, context, input);
        }

    }

    @Override
    public byte[] apply(final byte[] input, final List<Span> spans) {

        final String text = new String(input);
        final StringBuilder sb = new StringBuilder(text);

        LOGGER.info("Applying {} spans from the changeset.", spans.size());

        // Page numbers don't matter for plain text, so just loop over all spans.
        // The page number for each will be 0.

        // Sort the spans by start character.
        spans.sort((s1, s2) -> Integer.compare(s2.getCharacterStart(), s1.getCharacterStart()));

        for(final Span span : spans) {

            // Replace the text with the replacement.
            sb.delete(span.getCharacterStart(), span.getCharacterEnd());
            sb.insert(span.getCharacterStart(), span.getReplacement());

        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);

    }

}
