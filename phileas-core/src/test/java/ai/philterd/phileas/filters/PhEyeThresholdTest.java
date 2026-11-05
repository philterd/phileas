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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeDetector;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeDetectorProvider;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeFilter;
import ai.philterd.phileas.services.filters.ai.pheye.PhEyeSpan;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@code phEyeConfiguration.threshold} keeps a local detection only when it scores above the threshold,
 * and does not apply to the remote service.
 */
public class PhEyeThresholdTest {

    private static final String INPUT = "Ann met Bob and Cal and Dee.";

    // Scores the stub detector gives each name.
    private static final String[][] DETECTIONS = {{"Ann", "0.5"}, {"Bob", "0.6"}, {"Cal", "0.8"}, {"Dee", "0.95"}};

    @TempDir
    Path services;

    @Test
    public void defaultThresholdIsHalf() throws Exception {
        Assertions.assertEquals("Ann met {{{REDACTED-person}}} and {{{REDACTED-person}}} and {{{REDACTED-person}}}.",
                filterLocally(""));
    }

    @Test
    public void spanMustScoreAboveTheThreshold() throws Exception {
        Assertions.assertEquals("Ann met Bob and Cal and {{{REDACTED-person}}}.", filterLocally(", \"threshold\": 0.8"));
    }

    @Test
    public void remoteServiceIgnoresTheThreshold() throws Exception {

        final StringBuilder json = new StringBuilder("[");
        for (final PhEyeSpan span : spansFor(INPUT)) {
            json.append(json.length() > 1 ? "," : "").append(new Gson().toJson(span));
        }
        json.append("]");

        final HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(new StringEntity(json.toString()));
            return handler.handleResponse(response);
        });

        final PhEyeConfiguration configuration = new PhEyeConfiguration("http://localhost:18080");
        configuration.setThreshold(0.99);

        final PhEyeFilter filter = new PhEyeFilter(new FilterConfiguration.FilterConfigurationBuilder().build(),
                configuration, false, new HashMap<>(), FilterType.PERSON, httpClient);

        Assertions.assertEquals(4, filter.filter(new DefaultContextService(), null, "context", 0, INPUT).getSpans().size());

    }

    /** Runs the input through a policy with a local model, served by {@link StubProvider}. */
    private String filterLocally(final String thresholdSetting) throws Exception {

        final Path registration = services.resolve("META-INF/services/" + PhEyeDetectorProvider.class.getName());
        Files.createDirectories(registration.getParent());
        Files.writeString(registration, StubProvider.class.getName());

        final String json = "{ \"identifiers\": { \"person\": { \"phEyeConfiguration\": { "
                + "\"modelPath\": \"/models/stub\"" + thresholdSetting + " } } } }";

        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();

        try (URLClassLoader loader = new URLClassLoader(new URL[]{services.toUri().toURL()}, getClass().getClassLoader())) {

            thread.setContextClassLoader(loader);

            final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                    new DefaultContextService(), mock(VectorService.class), null);

            return service.filter(new Gson().fromJson(json, Policy.class), "context", INPUT).getFilteredText();

        } finally {
            thread.setContextClassLoader(original);
        }

    }

    private static List<PhEyeSpan> spansFor(final String text) {
        final List<PhEyeSpan> spans = new ArrayList<>();
        for (final String[] detection : DETECTIONS) {
            final PhEyeSpan span = new PhEyeSpan();
            span.setStart(text.indexOf(detection[0]));
            span.setEnd(text.indexOf(detection[0]) + detection[0].length());
            span.setText(detection[0]);
            span.setLabel("name");
            span.setScore(Double.parseDouble(detection[1]));
            spans.add(span);
        }
        return spans;
    }

    /** A local detector that returns every name in {@link #DETECTIONS} whatever the threshold. */
    public static class StubProvider implements PhEyeDetectorProvider {

        @Override
        public PhEyeDetector create(final PhEyeConfiguration configuration) {
            return (text, labels, context, piece) -> spansFor(text);
        }

    }

}
