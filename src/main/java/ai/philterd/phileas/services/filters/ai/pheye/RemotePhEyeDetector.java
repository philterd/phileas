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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * {@link PhEyeDetector} that performs detection by calling a remote PhEye service's
 * {@code /find} endpoint over HTTP. This is the default detector and preserves the
 * original PhEyeFilter behavior. The model's limits are read once from the service's
 * {@code /model} endpoint; a service without a usable one gets the {@link PhEyeDetector} defaults.
 */
public class RemotePhEyeDetector implements PhEyeDetector {

    private static final Logger LOGGER = LogManager.getLogger(RemotePhEyeDetector.class);

    private final PhEyeConfiguration phEyeConfiguration;
    private final HttpClient httpClient;
    private final Gson gson;

    /** How long after a failed read of {@code /model} the defaults are used without asking again. */
    static final long RETRY_NANOS = TimeUnit.SECONDS.toNanos(60);

    private final LongSupplier nanoTime;

    // {maxWords, maxWidth} from /model, or the defaults once the service has answered without usable limits.
    private volatile int[] limits;

    // When /model may be asked again after a failure that may pass, such as the service being down.
    private volatile long retryAt;
    private volatile boolean failed;

    public RemotePhEyeDetector(final PhEyeConfiguration phEyeConfiguration, final HttpClient httpClient) {
        this(phEyeConfiguration, httpClient, System::nanoTime);
    }

    RemotePhEyeDetector(final PhEyeConfiguration phEyeConfiguration, final HttpClient httpClient,
                        final LongSupplier nanoTime) {
        this.phEyeConfiguration = phEyeConfiguration;
        this.httpClient = httpClient;
        this.gson = new Gson();
        this.nanoTime = nanoTime;
    }

    @Override
    public List<PhEyeSpan> detect(final String text, final Collection<String> labels,
                                  final String context, final int piece) throws Exception {

        final PhEyeRequest phEyeRequest = new PhEyeRequest();
        phEyeRequest.setText(text);
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

        if (StringUtils.isNotEmpty(phEyeConfiguration.getBearerToken())) {
            httpPost.setHeader("Authorization", "Bearer " + phEyeConfiguration.getBearerToken());
        }

        final HttpClientResponseHandler<String> responseHandler = response -> {

            if (response.getCode() == 200) {

                final HttpEntity responseEntity = response.getEntity();
                return responseEntity != null ? EntityUtils.toString(responseEntity) : null;

            } else {

                // Ensure the connection is released even on error.
                EntityUtils.consume(response.getEntity());

                // The request to ph-eye was not successful.
                LOGGER.error("PhEye request failed with code {}", response.getCode());
                throw new IOException("Unable to process document. Received error response from ph-eye.");

            }

        };

        final String responseBody = httpClient.execute(httpPost, responseHandler);

        if (responseBody == null) {
            // We received null back which is not expected.
            throw new IOException("Unable to process document. Received error response from ph-eye.");
        }

        final Type listType = new TypeToken<ArrayList<PhEyeSpan>>() {}.getType();
        final List<PhEyeSpan> phEyeSpans = gson.fromJson(responseBody, listType);

        return phEyeSpans != null ? phEyeSpans : new ArrayList<>();

    }

    @Override
    public int maxWords() {
        return limits()[0];
    }

    @Override
    public int maxWidth() {
        return limits()[1];
    }

    private int[] limits() {
        final int[] known = limits;
        if (known != null) {
            return known;
        }
        if (failed && nanoTime.getAsLong() - retryAt < 0) {
            return defaults();
        }
        return readLimits();
    }

    private static int[] defaults() {
        return new int[] {DEFAULT_MAX_WORDS, DEFAULT_MAX_WIDTH};
    }

    /**
     * Reads the model's limits from {@code /model}. An answer without usable limits, such as a {@code 404} from a
     * service older than the endpoint, is kept as the defaults. A server error or a failed request, which may
     * pass, uses the defaults and is not asked again for {@link #RETRY_NANOS}.
     */
    private int[] readLimits() {

        if (httpClient == null) {
            return defaults();
        }

        try {

            final HttpGet httpGet = new HttpGet(new URIBuilder(phEyeConfiguration.getEndpoint() + "/model").build());
            httpGet.setConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(phEyeConfiguration.getTimeout(), TimeUnit.SECONDS)
                    .setResponseTimeout(phEyeConfiguration.getTimeout(), TimeUnit.SECONDS)
                    .build());
            httpGet.setHeader("Accept", "application/json");

            if (StringUtils.isNotEmpty(phEyeConfiguration.getBearerToken())) {
                httpGet.setHeader("Authorization", "Bearer " + phEyeConfiguration.getBearerToken());
            }

            final int[] read = httpClient.execute(httpGet, response -> {
                final String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : null;
                if (response.getCode() >= 500) {
                    throw new IOException("The PhEye service answered /model with " + response.getCode() + ".");
                }
                final int[] usable = response.getCode() == 200 ? usableLimits(body) : null;
                if (usable == null) {
                    LOGGER.info("The PhEye service has no usable /model endpoint (status {}); using windows of {} words.",
                            response.getCode(), defaults()[0]);
                    return defaults();
                }
                return usable;
            });

            limits = read;
            return read;

        } catch (final Exception e) {
            retryAt = nanoTime.getAsLong() + RETRY_NANOS;
            failed = true;
            LOGGER.warn("Could not read the PhEye service's model limits; using windows of {} words for now: {}",
                    defaults()[0], e.toString());
            return defaults();
        }

    }

    /** The limits in a {@code /model} body, or null when it does not give two positive integers. */
    private int[] usableLimits(final String body) {
        try {
            final ModelLimits model = body == null ? null : gson.fromJson(body, ModelLimits.class);
            if (model != null && model.maxWords != null && model.maxWords > 0
                    && model.maxWidth != null && model.maxWidth > 0) {
                return new int[] {model.maxWords, model.maxWidth};
            }
        } catch (final RuntimeException e) {
            // Not JSON of the expected shape.
        }
        return null;
    }

    /** The part of the PhEye service's {@code /model} response the detector uses. */
    private static final class ModelLimits {
        private Integer maxWords;
        private Integer maxWidth;
    }

}
