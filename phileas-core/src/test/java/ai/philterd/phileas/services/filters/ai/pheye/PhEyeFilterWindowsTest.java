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

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.mock;

/**
 * A PhEye filter detects over the whole text even when the model reads only its first words, using a
 * local detector that, like a GLiNER model, ignores everything after its first {@link #MAX_WORDS} words.
 */
public class PhEyeFilterWindowsTest {

    private static final int MAX_WORDS = 50;

    /** A name is one or two words, "Name1" or "Name1 Name2". */
    private static final Pattern NAME = Pattern.compile("Name\\d+(?: Name\\d+)?");

    private static final List<String> INPUTS = Collections.synchronizedList(new ArrayList<>());

    @TempDir
    Path services;

    @BeforeEach
    public void clear() {
        INPUTS.clear();
    }

    @Test
    public void textThatFitsIsDetectedInOneCallAsItIs() throws Exception {
        final String input = "Name1 Name2 met the clerk.";
        Assertions.assertEquals("{{{REDACTED-pheye}}} met the clerk.", filter(input, false));
        Assertions.assertEquals(List.of(input), INPUTS);
    }

    @Test
    public void namesAfterTheModelsLimitAreDetected() throws Exception {
        final String input = "Name1 met the clerk. " + "The report was filed. ".repeat(60) + "Later Name2 signed it.";

        Assertions.assertEquals("{{{REDACTED-pheye}}} met the clerk. " + "The report was filed. ".repeat(60)
                + "Later {{{REDACTED-pheye}}} signed it.", filter(input, false));
        Assertions.assertTrue(INPUTS.size() > 1);
    }

    @Test
    public void everyNameIsRedactedOnceAndWholeWhereverTheWindowsMeet() throws Exception {

        // Two-word names at every offset, so some cross every seam between windows.
        final StringBuilder input = new StringBuilder();
        int n = 0;
        for (int i = 0; i < 120; i++) {
            input.append("Name").append(n++).append(" Name").append(n++).append(" and ".repeat(i % 3)).append(", ");
        }

        final String expected = NAME.matcher(input).replaceAll("{{{REDACTED-pheye}}}");

        Assertions.assertEquals(expected, filter(input.toString(), false));
        Assertions.assertTrue(INPUTS.size() > 5);
    }

    @Test
    public void theFilterReportsEachNameOnceAtItsFullExtent() throws Exception {

        final StringBuilder input = new StringBuilder();
        int n = 0;
        for (int i = 0; i < 120; i++) {
            input.append("Name").append(n++).append(" Name").append(n++).append(" and ".repeat(i % 3)).append(", ");
        }

        final List<String> expected = new ArrayList<>();
        final Matcher names = NAME.matcher(input);
        while (names.find()) {
            expected.add(names.start() + "-" + names.end());
        }

        // Spans as the filter reports them, before Phileas resolves overlapping spans, which would hide a
        // duplicate or a name cut short at a window's edge.
        final List<String> reported = withProvider(() -> {
            final PhEyeConfiguration configuration = new PhEyeConfiguration("http://localhost:18080");
            configuration.setModelPath("/models/stub");
            configuration.setLabels(List.of("name"));
            final PhEyeFilter filter = new PhEyeFilter(new FilterConfiguration.FilterConfigurationBuilder().build(),
                    configuration, false, new HashMap<>(), FilterType.PH_EYE, null);
            return filter.filter(new DefaultContextService(), null, "context", 0, input.toString()).getSpans().stream()
                    .map(span -> span.getCharacterStart() + "-" + span.getCharacterEnd()).sorted(
                            Comparator.comparingInt(range -> Integer.parseInt(range.substring(0, range.indexOf('-')))))
                    .toList();
        });

        Assertions.assertEquals(expected, reported);
        Assertions.assertTrue(INPUTS.size() > 5);

    }

    @Test
    public void spansLineUpWithTheTextWhenPunctuationIsRemoved() throws Exception {
        final String input = "Name1, met the clerk! " + "The report: filed. ".repeat(30) + "Later (Name2) signed.";

        Assertions.assertEquals("{{{REDACTED-pheye}}}, met the clerk! " + "The report: filed. ".repeat(30)
                + "Later ({{{REDACTED-pheye}}}) signed.", filter(input, true));
    }

    /** Runs the input through a policy with the truncating local model, served by {@link TruncatingProvider}. */
    private String filter(final String input, final boolean removePunctuation) throws Exception {

        final String json = "{ \"identifiers\": { \"pheyes\": [ { \"removePunctuation\": " + removePunctuation
                + ", \"phEyeConfiguration\": { \"modelPath\": \"/models/stub\", \"labels\": [\"name\"] } } ] } }";

        return withProvider(() -> new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), mock(VectorService.class), null)
                .filter(new Gson().fromJson(json, Policy.class), "context", input).getFilteredText());

    }

    /** Runs the work with {@link TruncatingProvider} registered as the local detector provider. */
    private <T> T withProvider(final Callable<T> work) throws Exception {

        final Path registration = services.resolve("META-INF/services/" + PhEyeDetectorProvider.class.getName());
        Files.createDirectories(registration.getParent());
        Files.writeString(registration, TruncatingProvider.class.getName());

        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();

        try (URLClassLoader loader = new URLClassLoader(new URL[]{services.toUri().toURL()}, getClass().getClassLoader())) {
            thread.setContextClassLoader(loader);
            return work.call();
        } finally {
            thread.setContextClassLoader(original);
        }

    }

    /** A local detector that reads only its first {@link #MAX_WORDS} words, and reports the names among them. */
    public static class TruncatingProvider implements PhEyeDetectorProvider {

        @Override
        public PhEyeDetector create(final PhEyeConfiguration configuration) {

            return new PhEyeDetector() {

                @Override
                public List<PhEyeSpan> detect(final String text, final java.util.Collection<String> labels,
                                              final String context, final int piece) {

                    INPUTS.add(text);

                    // The end of the last word the model reads.
                    final Matcher words = PhEyeWindows.WORD.matcher(text);
                    int end = 0;
                    for (int i = 0; i < MAX_WORDS && words.find(); i++) {
                        end = words.end();
                    }

                    final List<PhEyeSpan> spans = new ArrayList<>();
                    final Matcher names = NAME.matcher(text.substring(0, end));
                    while (names.find()) {
                        final PhEyeSpan span = new PhEyeSpan();
                        span.setStart(names.start());
                        span.setEnd(names.end());
                        span.setText(names.group());
                        span.setLabel("name");
                        span.setScore(0.9);
                        spans.add(span);
                    }
                    return spans;

                }

                @Override
                public int maxWords() {
                    return MAX_WORDS;
                }

                @Override
                public int maxWidth() {
                    return 2;
                }

            };

        }

    }

}
