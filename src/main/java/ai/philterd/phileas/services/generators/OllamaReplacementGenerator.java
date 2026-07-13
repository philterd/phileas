/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.generators;

import ai.philterd.phileas.policy.Generator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * {@link ReplacementGenerator} that calls a local Ollama-compatible {@code /api/generate} endpoint
 * to produce a replacement value. The endpoint is expected to resolve inside the deployment boundary
 * so detected values are not sent to a third party.
 */
public class OllamaReplacementGenerator implements ReplacementGenerator {

    private final Generator generator;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public OllamaReplacementGenerator(final Generator generator, final HttpClient httpClient) {
        this.generator = generator;
        this.httpClient = httpClient;
    }

    @Override
    public String generate(final String token, final String label) throws Exception {

        final String prompt = StringUtils.defaultString(generator.getPrompt())
                .replace("{{token}}", token)
                .replace("{{label}}", StringUtils.defaultString(label));

        final JsonObject request = new JsonObject();
        request.addProperty("model", generator.getModel());
        request.addProperty("prompt", prompt);
        // Request a single, complete response rather than a stream of chunks so the body is one JSON object.
        request.addProperty("stream", false);

        final URI uri = new URIBuilder(generator.getEndpoint() + "/api/generate").build();

        // timeoutMs is required by the schema, so a generator can never block the pipeline indefinitely.
        final int timeoutMs = generator.getTimeoutMs();
        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .setResponseTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();

        final HttpPost httpPost = new HttpPost(uri);
        httpPost.setConfig(requestConfig);
        httpPost.setEntity(new StringEntity(gson.toJson(request)));
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Accept", "application/json");

        final HttpClientResponseHandler<String> responseHandler = response -> {
            final HttpEntity responseEntity = response.getEntity();
            if (response.getCode() == 200) {
                return responseEntity != null ? EntityUtils.toString(responseEntity) : null;
            }
            EntityUtils.consume(responseEntity);
            throw new IOException("Generator endpoint returned status " + response.getCode());
        };

        final String responseBody = httpClient.execute(httpPost, responseHandler);

        if (StringUtils.isBlank(responseBody)) {
            throw new IOException("Generator endpoint returned an empty response.");
        }

        final JsonObject responseObject = gson.fromJson(responseBody, JsonObject.class);
        if (responseObject == null || !responseObject.has("response")) {
            throw new IOException("Generator response did not contain a 'response' field.");
        }

        final String replacement = responseObject.get("response").getAsString();
        if (StringUtils.isBlank(replacement)) {
            throw new IOException("Generator produced a blank replacement.");
        }

        // Models often wrap the value in surrounding whitespace or newlines; return only the value.
        return replacement.trim();

    }

}
