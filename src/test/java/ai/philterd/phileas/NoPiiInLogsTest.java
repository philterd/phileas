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
package ai.philterd.phileas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Guards the invariant that no detected value (PII) is ever written to a log message. Phileas
 * processes documents that contain PII, so the text of a detected {@code Span} (and any other value
 * pulled from the input via {@code getText()}) must never be passed to a logger - not even at DEBUG,
 * and not via an exception whose message echoes the value.
 *
 * <p>This test fails the build if any logging statement under {@code src/main} passes a
 * {@code .getText()} call, which is the reliable accessor for input-derived text. Log the pattern,
 * the filter type, or a count instead - never the matched value. If a log statement legitimately
 * needs to call {@code getText()} on something that is provably not PII, refactor it so the value is
 * resolved into a clearly-named non-PII variable outside the logging call.
 */
public class NoPiiInLogsTest {

    private static final Path MAIN_SOURCE_ROOT = Path.of("src", "main", "java");

    // Matches a single LOGGER.<level>( ... ); call, spanning lines, up to the first ");".
    private static final Pattern LOGGER_CALL =
            Pattern.compile("LOGGER\\.(?:trace|debug|info|warn|error)\\(.*?\\);", Pattern.DOTALL);

    @Test
    public void noLoggingStatementLogsDetectedText() throws IOException {

        final List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(MAIN_SOURCE_ROOT)) {
            for (final Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".java"))::iterator) {
                final String source = Files.readString(path);
                final Matcher matcher = LOGGER_CALL.matcher(source);
                while (matcher.find()) {
                    if (matcher.group().contains(".getText()")) {
                        violations.add(path + ": " + matcher.group().replaceAll("\\s+", " "));
                    }
                }
            }
        }

        Assertions.assertTrue(violations.isEmpty(),
                "A logging statement passes a detected value (getText()) to the logger, which would write PII to logs:\n"
                        + String.join("\n", violations));
    }

}
