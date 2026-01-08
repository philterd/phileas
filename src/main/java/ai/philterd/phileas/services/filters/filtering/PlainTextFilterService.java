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
import ai.philterd.phileas.services.disambiguation.vector.VectorBasedSpanDisambiguationService;
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
                new VectorBasedSpanDisambiguationService(phileasConfiguration, vectorService),
                phileasConfiguration.incrementalRedactionsEnabled()
        );

    }

    @Override
    public TextFilterResult filter(final Policy policy, final String context, final String input) throws Exception {

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
        final List<PostFilter> postFilters = getPostFiltersForPolicy(policy);

        final TextFilterResult textFilterResult;

        // Do we need to split the input text due to its size?
        // Is the appliesToFilter = "*" or is at least one of the filters in the policy in the appliesToFilter list?
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

    @Override
    public byte[] apply(final byte[] input, final List<Span> spans) {

        final String text = new String(input);
        final StringBuilder sb = new StringBuilder(text);

        LOGGER.info("Applying {} spans from the changeset.", spans.size());

        // Page numbers don't matter for plain text, so just loop over all spans.
        // The page number for each will be 0.

        for(final Span span : spans) {

            // Replace the text with the replacement.
            sb.delete(span.getCharacterStart(), span.getCharacterEnd());
            sb.insert(span.getCharacterStart(), span.getReplacement());

        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);

    }

}
