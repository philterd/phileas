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
package ai.philterd.phileas.services.filters.ai.pheye;

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.dynamic.NerFilter;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.policy.Policy;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PhEyeFilter extends NerFilter {

    private static final Logger LOGGER = LogManager.getLogger(PhEyeFilter.class);

    private final boolean removePunctuation;

    private final PhEyeConfiguration phEyeConfiguration;
    private final Collection<String> labels;
    private final Gson gson;
    final PoolingHttpClientConnectionManager connectionManager;

    public PhEyeFilter(final FilterConfiguration filterConfiguration,
                       final PhEyeConfiguration phEyeConfiguration,
                       final boolean removePunctuation,
                       final Map<String, Double> thresholds) {

        super(filterConfiguration, thresholds, FilterType.AGE);

        this.phEyeConfiguration = phEyeConfiguration;
        this.removePunctuation = removePunctuation;
        this.labels = phEyeConfiguration.getLabels();
        this.gson = new Gson();

        this.connectionManager = new PoolingHttpClientConnectionManager();

        if(phEyeConfiguration.getMaxIdleConnections() > 0) {
            connectionManager.setMaxTotal(phEyeConfiguration.getMaxIdleConnections());
            connectionManager.setDefaultMaxPerRoute(phEyeConfiguration.getMaxIdleConnections());
        }

    }

    @Override
    public FilterResult filter(final Policy policy, final String context, final int piece,
                               final String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        // Remove punctuation if instructed to do so.
        // It is replacing each punctuation mark with an empty space. This will allow span indexes
        // to remain constant as opposed to removing the punctuation and causing the string to then
        // have a shorter length.
        final String formattedInput;
        if(removePunctuation) {
            formattedInput = input.replaceAll("\\p{Punct}", " ");
        } else {
            formattedInput = input;
        }
        
        final PhEyeRequest phEyeRequest = new PhEyeRequest();
        phEyeRequest.setText(input);
        phEyeRequest.setContext(context);
        phEyeRequest.setPiece(piece);
        phEyeRequest.setLabels(labels);

        final String json = gson.toJson(phEyeRequest);

        final URI uri = new URIBuilder(phEyeConfiguration.getEndpoint() + "/find")
                .build();

        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(phEyeConfiguration.getTimeout(), TimeUnit.SECONDS)
                .setResponseTimeout(phEyeConfiguration.getTimeout(), TimeUnit.SECONDS)
                .build();

        final HttpPost httpPost = new HttpPost(uri);
        httpPost.setConfig(requestConfig);
        httpPost.setEntity(new StringEntity(json));
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Accept", "application/json");

        if(StringUtils.isNotEmpty(phEyeConfiguration.getBearerToken())) {
            httpPost.setHeader("Authorization", "Bearer " + phEyeConfiguration.getBearerToken());
        }

        final HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(connectionManager);

        final CloseableHttpClient httpClient = httpClientBuilder.build();

        final HttpClientResponseHandler<String> responseHandler = response -> {

            if (response.getCode() == 200) {

                final HttpEntity responseEntity = response.getEntity();
                return responseEntity != null ? EntityUtils.toString(responseEntity) : null;

            } else {

                // The request to philter-ner was not successful.
                LOGGER.error("PhEyeFilter failed with code {}", response.getCode());
                throw new IOException("Unable to process document. Received error response from philter-ner.");

            }

        };

        final String responseBody = httpClient.execute(httpPost, responseHandler);

        if (responseBody != null) {

            final Type listType = new TypeToken<ArrayList<PhEyeSpan>>() {}.getType();
            final List<PhEyeSpan> phEyeSpans = new Gson().fromJson(responseBody, listType);

            if (CollectionUtils.isNotEmpty(phEyeSpans)) {

                for (final PhEyeSpan phEyeSpan : phEyeSpans) {

                    // Only interested in spans matching the tag we are looking for, e.g. PER, LOC.
                    if (labels.contains(phEyeSpan.getLabel())) {

                        // Check the confidence threshold.
                        if (!thresholds.containsKey(phEyeSpan.getLabel().toUpperCase()) || phEyeSpan.getScore() >= thresholds.get(phEyeSpan.getLabel().toUpperCase())) {

                            // Get the window of text surrounding the token.
                            final String[] window = getWindow(formattedInput, phEyeSpan.getStart(), phEyeSpan.getEnd());

                            // Currently only PERSON type is supported.
                            final FilterType filterType = FilterType.PERSON;

                            final Span span = createSpan(policy, context, filterType, phEyeSpan.getText(),
                                    window, phEyeSpan.getLabel(), phEyeSpan.getStart(), phEyeSpan.getEnd(),
                                    phEyeSpan.getScore());

                            // Span will be null if no span was created due to it being excluded.
                            if (span != null) {
                                spans.add(span);
                            }

                        }

                    }

                }

            }

            LOGGER.debug("Returning {} NER spans from ph-eye.", spans.size());
            return new FilterResult(context, piece, spans);

        } else {

            // We received null back which is not expected.
            throw new IOException("Unable to process document. Received error response from philter-ner.");

        }

    }

    private Span createSpan(final Policy policy, final String context,
                            final FilterType filterType, final String text, final  String[] window,
                            final String classification, final int start, final int end, final double confidence) throws Exception {

        final Replacement replacement = getReplacement(policy, context, text, window, confidence, classification, null);

        if(StringUtils.equals(replacement.getReplacement(), text)) {

            // If the replacement is the same as the token then there is no span.
            // A condition in the strategy excluded it.

            return null;

        } else {

            // Is this term ignored?
            final boolean ignored = isIgnored(text);

            return Span.make(start, end, filterType, context, confidence, text,
                    replacement.getReplacement(), replacement.getSalt(), ignored, replacement.isApplied(), window, priority);

        }

    }

}