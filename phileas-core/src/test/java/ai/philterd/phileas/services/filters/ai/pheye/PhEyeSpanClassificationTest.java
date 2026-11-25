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
package ai.philterd.phileas.services.filters.ai.pheye;

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.strategies.ai.PhEyeFilterStrategy;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A PhEye span's classification is the label the model gave it, whether the span came from the PhEye service or
 * from in-process detection, and setting it does not change what {@code %t} prints in a redaction format.
 */
public class PhEyeSpanClassificationTest {

    private static final String INPUT = "Ann was seen at Mercy.";

    // The label the stub local detector gives its span.
    private static final AtomicReference<String> LOCAL_LABEL = new AtomicReference<>();

    @TempDir
    Path services;

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource(delimiter = '|', value = {
            // label | detected text | filter type
            "name     | Ann   | PERSON",
            "Person   | Ann   | PERSON",
            "hospital | Mercy | OTHER"
    })
    public void aSpanFromThePhEyeServiceIsClassifiedByItsLabel(final String label, final String text,
                                                               final FilterType filterType) throws Exception {

        final Span span = onlySpan(remoteFilter(label, text));

        Assertions.assertEquals(label, span.getClassification());
        Assertions.assertEquals(filterType, span.getFilterType());
        Assertions.assertEquals("{{{REDACTED-" + label.toLowerCase() + "}}}", span.getReplacement());

    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource(delimiter = '|', value = {
            // label | detected text | filter type
            "name     | Ann   | PERSON",
            "Person   | Ann   | PERSON",
            "hospital | Mercy | OTHER"
    })
    public void aSpanFromInProcessDetectionIsClassifiedByItsLabel(final String label, final String text,
                                                                  final FilterType filterType) throws Exception {

        final Span span = onlySpan(localFilter(label, text));

        Assertions.assertEquals(label, span.getClassification());
        Assertions.assertEquals(filterType, span.getFilterType());
        Assertions.assertEquals("{{{REDACTED-" + label.toLowerCase() + "}}}", span.getReplacement());

    }

    private static Span onlySpan(final List<Span> spans) {
        Assertions.assertEquals(1, spans.size(), spans.toString());
        return spans.get(0);
    }

    private static FilterConfiguration filterConfiguration() {

        final PhEyeFilterStrategy strategy = new PhEyeFilterStrategy();
        strategy.setStrategy("REDACT");
        strategy.setRedactionFormat("{{{REDACTED-%t}}}");

        return new FilterConfiguration.FilterConfigurationBuilder().withStrategies(List.of(strategy)).build();

    }

    /** The spans a filter calling a PhEye service finds when the service returns one span of the label. */
    private static List<Span> remoteFilter(final String label, final String text) throws Exception {

        final int start = INPUT.indexOf(text);
        final String response = "[{\"start\": " + start + ", \"end\": " + (start + text.length()) + ", \"label\": \""
                + label + "\", \"text\": \"" + text + "\", \"score\": 0.9}]";

        final HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse httpResponse = mock(ClassicHttpResponse.class);
            when(httpResponse.getCode()).thenReturn(200);
            when(httpResponse.getEntity()).thenReturn(new StringEntity(response));
            return handler.handleResponse(httpResponse);
        });

        final PhEyeConfiguration configuration = new PhEyeConfiguration("http://localhost:18080");
        configuration.setLabels(List.of(label));

        final PhEyeFilter filter = new PhEyeFilter(filterConfiguration(), configuration, false, new HashMap<>(),
                FilterType.PH_EYE, httpClient);

        return filter.filter(new DefaultContextService(), null, "context", 0, INPUT).getSpans();

    }

    /** The spans a filter with a local model finds when the model, {@link StubProvider}, returns one span of the label. */
    private List<Span> localFilter(final String label, final String text) throws Exception {

        LOCAL_LABEL.set(label + "|" + text);

        final Path registration = services.resolve("META-INF/services/" + PhEyeDetectorProvider.class.getName());
        Files.createDirectories(registration.getParent());
        Files.writeString(registration, StubProvider.class.getName());

        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();

        try (URLClassLoader loader = new URLClassLoader(new URL[]{services.toUri().toURL()}, getClass().getClassLoader())) {

            thread.setContextClassLoader(loader);

            final PhEyeConfiguration configuration = new PhEyeConfiguration("http://localhost:18080");
            configuration.setModelPath("/models/stub");
            configuration.setLabels(List.of(label));

            final PhEyeFilter filter = new PhEyeFilter(filterConfiguration(), configuration, false, new HashMap<>(),
                    FilterType.PH_EYE, null);

            return filter.filter(new DefaultContextService(), null, "context", 0, INPUT).getSpans();

        } finally {
            thread.setContextClassLoader(original);
        }

    }

    /** A local detector that returns one span, of the label and text set in {@link #LOCAL_LABEL}. */
    public static class StubProvider implements PhEyeDetectorProvider {

        @Override
        public PhEyeDetector create(final PhEyeConfiguration configuration) {

            return new PhEyeDetector() {

                @Override
                public List<PhEyeSpan> detect(final String text, final Collection<String> labels,
                                              final String context, final int piece) {

                    final String[] detection = LOCAL_LABEL.get().split("\\|");

                    final PhEyeSpan span = new PhEyeSpan();
                    span.setStart(text.indexOf(detection[1]));
                    span.setEnd(text.indexOf(detection[1]) + detection[1].length());
                    span.setText(detection[1]);
                    span.setLabel(detection[0]);
                    span.setScore(0.9);
                    return List.of(span);

                }

            };

        }

    }

}
