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
package ai.philterd.phileas.services.rtf;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.RtfFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static ai.philterd.phileas.services.rtf.RtfDocs.EMAIL;
import static ai.philterd.phileas.services.rtf.RtfDocs.doc;

/**
 * Redacts a corpus of documents with each setting's values and converts every output to text with LibreOffice,
 * which must read it and find the text redaction kept. Skipped where LibreOffice is not installed.
 */
public class RtfOpensInLibreOfficeTest {

    private static final String KEEP = "{\"comments\":\"redact\",\"trackedChanges\":\"redact\",\"hiddenText\":\"redact\","
            + "\"headersFooters\":\"keep\",\"uninspectableObjects\":\"keep\",\"removeDocumentProperties\":false}";

    static Map<String, String> corpus() {
        final Map<String, String> corpus = new LinkedHashMap<>();
        corpus.put("body", doc("Email {\\b " + EMAIL + "} today. Caf\\'e9 \\u8217?s.\\par Second paragraph.\\par "));
        corpus.put("split", doc("Mail jane.doe{\\b \\'40exam}ple.com now.\\par "));
        corpus.put("table", doc("\\trowd\\cellx2000\\cellx4000\\intbl Email\\cell " + EMAIL + "\\cell\\row\\pard After table.\\par "));
        corpus.put("notes", doc("See note{\\footnote\\pard Contact " + EMAIL + ".}.\\par "));
        corpus.put("header", doc("{\\header\\pard Header " + EMAIL + "\\par}Body text.\\par "));
        corpus.put("field", doc("{\\field{\\*\\fldinst HYPERLINK \"mailto:" + EMAIL + "\"}{\\fldrslt write us}}\\par "));
        corpus.put("everything", RtfDocs.everything());
        corpus.put("object", doc("Before {\\object\\objemb{\\*\\objclass Package}{\\*\\objdata 0102}{\\result {\\pict\\wmetafile8 0102}}} after.\\par "));
        return corpus;
    }

    private static boolean libreOffice() {
        try {
            final Process process = new ProcessBuilder("soffice", "--version").redirectErrorStream(true).start();
            return process.waitFor(60, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (final Exception e) {
            return false;
        }
    }

    @Test
    public void everyOutputOpensInLibreOffice() throws Exception {

        Assumptions.assumeTrue(libreOffice(), "LibreOffice is not installed");

        final Path root = Files.createDirectories(Path.of("target", "rtf-validation"));
        final Path converted = Files.createDirectories(root.resolve("txt"));
        final RtfFilterService service = new RtfFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);

        final List<String> files = new ArrayList<>();
        for (final Map.Entry<String, String> entry : corpus().entrySet()) {
            for (final String word : List.of("{}", KEEP)) {
                final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}},\"config\":{\"word\":"
                        + word + "}}", Policy.class);
                final String name = entry.getKey() + (word.equals("{}") ? "-defaults" : "-keep") + ".rtf";
                final byte[] output = service.filter(policy, "ctx", RtfDocs.bytes(entry.getValue()), MimeType.APPLICATION_RTF)
                        .getDocument();
                Files.write(root.resolve(name), output);
                files.add(root.resolve(name).toAbsolutePath().toString());
            }
        }

        final List<String> command = new ArrayList<>(List.of("soffice",
                "-env:UserInstallation=" + root.resolve("profile").toAbsolutePath().toUri(),
                "--headless", "--convert-to", "txt:Text (encoded):UTF8", "--outdir", converted.toAbsolutePath().toString()));
        command.addAll(files);
        final Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(root.resolve("soffice.log").toFile()).start();
        Assertions.assertTrue(process.waitFor(5, TimeUnit.MINUTES), "LibreOffice did not finish");
        Assertions.assertEquals(0, process.exitValue(), Files.readString(root.resolve("soffice.log")));

        for (final String file : files) {
            final String name = new File(file).getName().replace(".rtf", ".txt");
            final Path text = converted.resolve(name);
            Assertions.assertTrue(Files.exists(text), name + " was not converted");
            final String content = Files.readString(text, StandardCharsets.UTF_8);
            Assertions.assertFalse(content.contains(EMAIL), name + ": " + content);
            // Every word of the first line Phileas reads is in what LibreOffice read. LibreOffice's text export
            // leaves out notes and writes footnote marks, so only the first line is compared, word by word.
            final String expected = RtfTextExtractor.extract(Files.readString(Path.of(file), StandardCharsets.ISO_8859_1));
            for (final String word : expected.split("\n")[0].split("[^\\p{L}]+")) {
                if (!word.isEmpty()) {
                    Assertions.assertTrue(content.contains(word), name + " is missing \"" + word + "\": " + content);
                }
            }
        }

    }

}
