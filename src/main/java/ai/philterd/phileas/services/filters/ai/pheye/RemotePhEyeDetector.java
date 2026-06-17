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

/**
 * {@link PhEyeDetector} that performs detection by calling a remote PhEye service's
 * {@code /find} endpoint over HTTP. This is the default detector and preserves the
 * original PhEyeFilter behavior.
 */
public class RemotePhEyeDetector implements PhEyeDetector {

    private static final Logger LOGGER = LogManager.getLogger(RemotePhEyeDetector.class);

    private final PhEyeConfiguration phEyeConfiguration;
    private final HttpClient httpClient;
    private final Gson gson;

    public RemotePhEyeDetector(final PhEyeConfiguration phEyeConfiguration, final HttpClient httpClient) {
        this.phEyeConfiguration = phEyeConfiguration;
        this.httpClient = httpClient;
        this.gson = new Gson();
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

}
