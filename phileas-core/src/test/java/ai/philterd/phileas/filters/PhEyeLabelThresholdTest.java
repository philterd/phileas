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
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A PhEye policy's thresholds: the detector is given the lowest one configured, so it does not drop a span a
 * label's threshold would keep, and each label's threshold is applied to the spans it returns.
 */
public class PhEyeLabelThresholdTest {

    private static final String INPUT = "Ann met Bob and Cal and Dee.";

    // Scores the stub detector gives each name.
    private static final String[][] DETECTIONS = {{"Ann", "0.35"}, {"Bob", "0.45"}, {"Cal", "0.6"}, {"Dee", "0.95"}};

    // The threshold the stub detector was last given.
    private static final AtomicReference<Double> RECEIVED = new AtomicReference<>();

    // Every threshold the windowed stub detector was given.
    private static final List<Double> RECEIVED_BY_WINDOW = new java.util.concurrent.CopyOnWriteArrayList<>();

    @TempDir
    Path services;

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource(delimiter = '|', value = {
            // phEyeConfiguration and thresholds settings | redacted names | threshold the detector is given
            "''                                         | Cal Dee         | 0.5",
            "'\"threshold\": 0.3'                       | Ann Bob Cal Dee | 0.3",
            "'\"threshold\": 0.6'                       | Dee             | 0.6",
            "'thresholds: name 0.3'                     | Ann Bob Cal Dee | 0.3",
            "'thresholds: NAME 0.3'                     | Ann Bob Cal Dee | 0.3",
            "'thresholds: name 0.9'                     | Dee             | 0.9",
            "'thresholds: name 0.6'                     | Dee             | 0.6",
            "'\"threshold\": 0.9, thresholds: name 0.4' | Bob Cal Dee     | 0.4"
    })
    public void eachLabelsThresholdApplies(final String settings, final String redacted, final double given) throws Exception {

        Assertions.assertEquals(expected(redacted), filterLocally(settings));
        Assertions.assertEquals(given, RECEIVED.get());

    }

    @Test
    public void everyWindowOfALongTextIsGivenTheThreshold() throws Exception {

        RECEIVED_BY_WINDOW.clear();

        Assertions.assertEquals(expected("Ann Bob Cal Dee"), filterLocally("thresholds: name 0.3", WindowedStubProvider.class));
        Assertions.assertTrue(RECEIVED_BY_WINDOW.size() > 1, "the text should be divided into windows");
        Assertions.assertTrue(RECEIVED_BY_WINDOW.stream().allMatch(t -> t != null && t == 0.3), RECEIVED_BY_WINDOW.toString());

    }

    @Test
    public void theRemoteRequestCarriesTheLowestLabelThreshold() throws Exception {

        Assertions.assertEquals(0.3, remoteRequest(Map.of("name", 0.3)).get("threshold").getAsDouble());

    }

    @Test
    public void theRemoteRequestHasNoThresholdWhenNoneIsConfigured() throws Exception {

        // The model's own default then applies, as before.
        Assertions.assertFalse(remoteRequest(Map.of()).has("threshold"));

    }

    /** The JSON body PhEyeFilter sends to a remote PhEye for the given per-label thresholds. */
    private static com.google.gson.JsonObject remoteRequest(final Map<String, Double> thresholds) throws Exception {

        final AtomicReference<String> body = new AtomicReference<>();
        final HttpClient httpClient = mock(HttpClient.class);

        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            body.set(EntityUtils.toString(((ClassicHttpRequest) invocation.getArgument(0)).getEntity()));
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(new StringEntity("[]"));
            return handler.handleResponse(response);
        });

        final PhEyeConfiguration configuration = new PhEyeConfiguration("http://localhost:18080");
        configuration.setLabels(List.of("name"));

        final PhEyeFilter filter = new PhEyeFilter(new FilterConfiguration.FilterConfigurationBuilder().build(),
                configuration, false, thresholds, FilterType.PERSON, httpClient);

        filter.filter(new DefaultContextService(), null, "context", 0, INPUT);

        return new Gson().fromJson(body.get(), com.google.gson.JsonObject.class);

    }

    private static String expected(final String redacted) {
        String text = INPUT;
        for (final String name : redacted.split(" ")) {
            text = text.replace(name, "{{{REDACTED-person}}}");
        }
        return text;
    }

    /** Runs the input through a policy with a local model, served by {@link StubProvider}. */
    private String filterLocally(final String settings) throws Exception {
        return filterLocally(settings, StubProvider.class);
    }

    private String filterLocally(final String settings, final Class<? extends PhEyeDetectorProvider> provider) throws Exception {

        final Path registration = services.resolve("META-INF/services/" + PhEyeDetectorProvider.class.getName());
        Files.createDirectories(registration.getParent());
        Files.writeString(registration, provider.getName());

        // "thresholds: <label> <value>" sets one per-label threshold; anything else goes in phEyeConfiguration.
        String configuration = settings;
        String thresholds = "";
        final int at = settings.indexOf("thresholds:");
        if (at >= 0) {
            final String[] parts = settings.substring(at + "thresholds:".length()).trim().split(" ");
            thresholds = ", \"thresholds\": { \"" + parts[0] + "\": " + parts[1] + " }";
            configuration = settings.substring(0, at).replaceAll(",\\s*$", "");
        }

        final String json = "{ \"identifiers\": { \"person\": { \"phEyeConfiguration\": { \"modelPath\": \"/models/stub\", "
                + "\"labels\": [ \"name\" ]" + (configuration.isBlank() ? "" : ", " + configuration) + " }"
                + thresholds + " } } }";

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

    /** A local detector that, like the real one, returns the names scoring above the threshold it is given. */
    public static class StubProvider implements PhEyeDetectorProvider {

        @Override
        public PhEyeDetector create(final PhEyeConfiguration configuration) {

            return new PhEyeDetector() {

                @Override
                public List<PhEyeSpan> detect(final String text, final Collection<String> labels,
                                              final String context, final int piece) {
                    return detect(text, labels, null, context, piece);
                }

                @Override
                public List<PhEyeSpan> detect(final String text, final Collection<String> labels, final Double threshold,
                                              final String context, final int piece) {

                    RECEIVED.set(threshold);
                    return detections(text, threshold);

                }

            };

        }

    }

    /** A stub detector whose model reads 4 words at a time, so a longer text is detected in windows. */
    public static class WindowedStubProvider implements PhEyeDetectorProvider {

        @Override
        public PhEyeDetector create(final PhEyeConfiguration configuration) {

            return new PhEyeDetector() {

                @Override
                public List<PhEyeSpan> detect(final String text, final Collection<String> labels,
                                              final String context, final int piece) {
                    return detect(text, labels, null, context, piece);
                }

                @Override
                public List<PhEyeSpan> detect(final String text, final Collection<String> labels, final Double threshold,
                                              final String context, final int piece) {
                    RECEIVED_BY_WINDOW.add(threshold == null ? Double.NaN : threshold);
                    return detections(text, threshold);
                }

                @Override
                public int maxWords() {
                    return 4;
                }

                @Override
                public int maxWidth() {
                    return 1;
                }

            };

        }

    }

    /** The names in the text scoring above the threshold, or above 0.5 with none, as the real detectors cut. */
    private static List<PhEyeSpan> detections(final String text, final Double threshold) {

        final double cut = threshold == null ? 0.5 : threshold;

        final List<PhEyeSpan> spans = new ArrayList<>();
        for (final String[] detection : DETECTIONS) {
            if (text.contains(detection[0]) && Double.parseDouble(detection[1]) > cut) {
                final PhEyeSpan span = new PhEyeSpan();
                span.setStart(text.indexOf(detection[0]));
                span.setEnd(text.indexOf(detection[0]) + detection[0].length());
                span.setText(detection[0]);
                span.setLabel("name");
                span.setScore(Double.parseDouble(detection[1]));
                spans.add(span);
            }
        }
        return spans;

    }

}
