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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.filters.ai.pheye.MissingPhEyeProviderException;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeFilter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PhEyeTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        final boolean removePunctuation = false;
        final Map<String, Double> thresholds = new HashMap<>();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final HttpClient httpClient = mock(HttpClient.class);
        final String jsonResponse = "[{\"start\": 0, \"end\": 17, \"label\": \"name\", \"text\": \"George Washington\", \"score\": 1.0}]";

        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            final HttpEntity entity = new StringEntity(jsonResponse);

            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(entity);

            return handler.handleResponse(response);
        });

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration, phEyeConfiguration, removePunctuation, thresholds, FilterType.PERSON, httpClient);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "George Washington was the first president.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("George Washington", filtered.getSpans().iterator().next().getText());

    }

    @Test
    public void filter2() throws Exception {

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        final boolean removePunctuation = false;
        final Map<String, Double> thresholds = new HashMap<>();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final HttpClient httpClient = mock(HttpClient.class);
        final String jsonResponse = "[]";

        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            final HttpEntity entity = new StringEntity(jsonResponse);

            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(entity);

            return handler.handleResponse(response);
        });

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration, phEyeConfiguration, removePunctuation, thresholds, FilterType.PERSON, httpClient);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "No name here was the first president.");

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void multipleFilterCalls() throws Exception {

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        final boolean removePunctuation = false;
        final Map<String, Double> thresholds = new HashMap<>();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final HttpClient httpClient = mock(HttpClient.class);
        final String jsonResponse1 = "[{\"start\": 0, \"end\": 17, \"label\": \"name\", \"text\": \"George Washington\", \"score\": 1.0}]";
        final String jsonResponse2 = "[]";

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration, phEyeConfiguration, removePunctuation, thresholds, FilterType.PERSON, httpClient);

        // This is to test the http connection pooling for connections to ph-eye.
        for(int x = 0; x < 10; x++) {

            // Mock for filtered1
            when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(invocation -> {
                final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
                final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
                final HttpEntity entity = new StringEntity(jsonResponse1);
                when(response.getCode()).thenReturn(200);
                when(response.getEntity()).thenReturn(entity);
                return handler.handleResponse(response);
            });

            final Filtered filtered1 = filter.filter(getPolicy(), "context", PIECE, "George Washington was the first president.");
            Assertions.assertEquals(1, filtered1.getSpans().size());
            Assertions.assertEquals("George Washington", filtered1.getSpans().iterator().next().getText());

            // Mock for filtered2
            when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(invocation -> {
                final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
                final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
                final HttpEntity entity = new StringEntity(jsonResponse2);
                when(response.getCode()).thenReturn(200);
                when(response.getEntity()).thenReturn(entity);
                return handler.handleResponse(response);
            });

            final Filtered filtered2 = filter.filter(getPolicy(), "context", PIECE, "No name here was the first president.");
            Assertions.assertEquals(0, filtered2.getSpans().size());

        }

    }

    @Test
    public void removePunctuationStripsPunctuationFromModelInput() throws Exception {

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        final boolean removePunctuation = true;
        final Map<String, Double> thresholds = new HashMap<>();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final HttpClient httpClient = mock(HttpClient.class);
        final String[] sentText = new String[1];

        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpPost post = invocation.getArgument(0);
            sentText[0] = new Gson().fromJson(EntityUtils.toString(post.getEntity()), JsonObject.class).get("text").getAsString();

            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(new StringEntity("[]"));
            return handler.handleResponse(response);
        });

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration, phEyeConfiguration, removePunctuation, thresholds, FilterType.PERSON, httpClient);

        final String input = "George Washington, the first president.";
        filter.filter(getPolicy(), "context", PIECE, input);

        // With removePunctuation enabled, the text sent to the model has its punctuation removed, and
        // it remains the same length (each punctuation mark is replaced with a space) so span offsets
        // still line up with the original input.
        Assertions.assertNotNull(sentText[0]);
        Assertions.assertFalse(sentText[0].matches(".*\\p{Punct}.*"), "Sent text still contains punctuation: " + sentText[0]);
        Assertions.assertEquals(input.length(), sentText[0].length());

    }

    @Test
    public void modelInputUnchangedWhenRemovePunctuationDisabled() throws Exception {

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        final boolean removePunctuation = false;
        final Map<String, Double> thresholds = new HashMap<>();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final HttpClient httpClient = mock(HttpClient.class);
        final String[] sentText = new String[1];

        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpPost post = invocation.getArgument(0);
            sentText[0] = new Gson().fromJson(EntityUtils.toString(post.getEntity()), JsonObject.class).get("text").getAsString();

            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(new StringEntity("[]"));
            return handler.handleResponse(response);
        });

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration, phEyeConfiguration, removePunctuation, thresholds, FilterType.PERSON, httpClient);

        final String input = "George Washington, the first president.";
        filter.filter(getPolicy(), "context", PIECE, input);

        // With removePunctuation disabled, the original text (including punctuation) is sent unchanged.
        Assertions.assertEquals(input, sentText[0]);

    }

    @Test
    public void nameLabelMapsToPersonFilterType() throws Exception {
        // The ph-eye-pii-en-* models label person names "name". A span returned
        // with the "name" label must classify as FilterType.PERSON.
        final Filtered filtered = filterWithMockLabel("name", null);
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(FilterType.PERSON, filtered.getSpans().iterator().next().getFilterType());
    }

    @Test
    public void personLabelStillMapsToPersonFilterType() throws Exception {
        // Backward compatibility: the older "Person" label must still classify as PERSON.
        final Filtered filtered = filterWithMockLabel("Person", java.util.List.of("Person"));
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(FilterType.PERSON, filtered.getSpans().iterator().next().getFilterType());
    }

    @Test
    public void modelPathWithoutProviderFailsClearly() {
        // A policy that asks for local inference (modelPath set) but without the optional
        // phileas-pheye-onnx module on the classpath must fail with a dedicated exception
        // whose message points the user at the missing dependency, rather than silently
        // falling back to the remote service. The failure occurs while the filter is built.
        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        phEyeConfiguration.setModelPath("/models/ph-eye-pii-en-small");

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final HttpClient httpClient = mock(HttpClient.class);

        final MissingPhEyeProviderException exception = Assertions.assertThrows(
                MissingPhEyeProviderException.class,
                () -> new PhEyeFilter(filterConfiguration, phEyeConfiguration, false,
                        new HashMap<>(), FilterType.PERSON, httpClient));

        Assertions.assertTrue(exception.getMessage().contains("phileas-pheye-onnx"));
        Assertions.assertTrue(exception.getMessage().contains("/models/ph-eye-pii-en-small"));
    }

    // Runs a PhEyeFilter against a mocked ph-eye response carrying a single span
    // with the given label. When requestedLabels is null the filter uses its
    // configured default labels.
    private Filtered filterWithMockLabel(final String label, final java.util.Collection<String> requestedLabels) throws Exception {

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration("http://localhost:18080");
        if (requestedLabels != null) {
            phEyeConfiguration.setLabels(requestedLabels);
        }

        final boolean removePunctuation = false;
        final Map<String, Double> thresholds = new HashMap<>();

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final HttpClient httpClient = mock(HttpClient.class);
        final String jsonResponse = "[{\"start\": 0, \"end\": 17, \"label\": \"" + label + "\", \"text\": \"George Washington\", \"score\": 1.0}]";

        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            final HttpEntity entity = new StringEntity(jsonResponse);

            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(entity);

            return handler.handleResponse(response);
        });

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration, phEyeConfiguration, removePunctuation, thresholds, FilterType.PERSON, httpClient);

        return filter.filter(getPolicy(), "context", PIECE, "George Washington was the first president.");
    }

}
