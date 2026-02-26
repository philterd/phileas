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
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeFilter;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
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
        final String jsonResponse = "[{\"start\": 0, \"end\": 17, \"label\": \"Person\", \"text\": \"George Washington\", \"score\": 1.0}]";

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
        final String jsonResponse1 = "[{\"start\": 0, \"end\": 17, \"label\": \"Person\", \"text\": \"George Washington\", \"score\": 1.0}]";
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

}
