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
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.Explanation;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.model.filtering.TextSegment;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import ai.philterd.phileas.services.filters.filtering.RtfFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import static ai.philterd.phileas.services.rtf.RtfDocs.EMAIL;
import static ai.philterd.phileas.services.rtf.RtfDocs.OTHER_EMAIL;
import static ai.philterd.phileas.services.rtf.RtfDocs.bytes;
import static ai.philterd.phileas.services.rtf.RtfDocs.doc;

public class RtfTextOutputTest {

    private static final FilterOptions TEXT = FilterOptions.defaults().withOutput(FilterOptions.Output.TEXT);

    private static final String KEEP_ALL = "{\"comments\":\"redact\",\"trackedChanges\":\"redact\",\"hiddenText\":\"redact\"}";

    private static final RtfFilterService SERVICE = new RtfFilterService(new PhileasConfiguration(new Properties()),
            new DefaultContextService(), new InMemoryVectorService(), null);

    private static Policy policy(final String word) {
        return policy(word, null);
    }

    private static Policy policy(final String word, final String review) {
        final List<String> config = new ArrayList<>();
        if (word != null) {
            config.add("\"word\":" + word);
        }
        if (review != null) {
            config.add("\"review\":" + review);
        }
        return new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}"
                + (config.isEmpty() ? "" : ",\"config\":{" + String.join(",", config) + "}") + "}", Policy.class);
    }

    private static BinaryDocumentFilterResult filter(final Policy policy, final String rtf, final FilterOptions options)
            throws Exception {
        return SERVICE.filter(policy, new DefaultContextService(), new InMemoryVectorService(), "ctx", bytes(rtf),
                MimeType.APPLICATION_RTF, options);
    }

    private static TextSegment segment(final BinaryDocumentFilterResult result, final String label) {
        return result.getText().getSegments().stream().filter(s -> s.getLabel().equals(label)).findFirst()
                .orElseThrow(() -> new AssertionError(label + " in " + result.getText()));
    }

    private static List<String> labels(final BinaryDocumentFilterResult result) {
        return result.getText().getSegments().stream().map(TextSegment::getLabel).toList();
    }

    @Test
    public void bodyIsOneParagraphPerLineInPlaceOfTheDocument() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy(null),
                doc("Email {\\b " + EMAIL + "} today. Caf\\'e9 \\u8217?s.\\par Second paragraph.\\par "), TEXT);

        Assertions.assertNull(result.getDocument());
        Assertions.assertEquals(List.of(new TextSegment("body",
                "Email {{{REDACTED-email-address}}} today. Caf\u00e9 \u2019s.\nSecond paragraph.", TextSegment.Status.TEXT)),
                result.getText().getSegments());
        Assertions.assertEquals(1, result.getExplanation().appliedSpans().size());

    }

    @Test
    public void aValueSplitAcrossRunsAndEscapesIsReplacedWhole() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy(null),
                doc("Mail jane.doe{\\b \\'40exam}ple.com now.\\par "), TEXT);

        Assertions.assertEquals("Mail {{{REDACTED-email-address}}} now.", segment(result, "body").getText());

    }

    @Test
    public void tableCellsAreLines() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy(null), doc("\\trowd\\cellx2000\\cellx4000\\intbl Email\\cell "
                + EMAIL + "\\cell\\row\\pard After table.\\par "), TEXT);

        Assertions.assertEquals("Email\n{{{REDACTED-email-address}}}\nAfter table.", segment(result, "body").getText());

    }

    @Test
    public void eachLineIsTheRedactedDocumentsText() throws Exception {

        for (final Map.Entry<String, String> entry : RtfOpensInLibreOfficeTest.corpus().entrySet()) {
            for (final String word : new String[]{null, KEEP_ALL}) {

                final BinaryDocumentFilterResult text = filter(policy(word), entry.getValue(), TEXT);
                final BinaryDocumentFilterResult document = filter(policy(word), entry.getValue(), FilterOptions.defaults());

                final List<String> redacted = RtfRedactor.readParagraphs(document.getDocument(), policy(word).getConfig().getWord());
                final String all = text.getText().join("\n");
                Assertions.assertFalse(all.contains(EMAIL) || all.contains(OTHER_EMAIL), entry.getKey() + ": " + all);
                for (final String line : all.split("\n")) {
                    Assertions.assertTrue(line.isEmpty() || redacted.contains(line),
                            entry.getKey() + " " + word + ": <" + line + "> not in " + redacted);
                }

            }
        }

    }

    @Test
    public void footnotesHeadersCommentsAndDeletionsAreLabeled() throws Exception {

        Assertions.assertEquals("Contact {{{REDACTED-email-address}}}.",
                segment(filter(policy(null), doc("See note{\\footnote\\pard Contact " + EMAIL + ".}.\\par "), TEXT),
                        "footnotes").getText());

        final String header = doc("{\\header\\pard Header " + EMAIL + "\\par}Body text.\\par ");
        Assertions.assertEquals("Header {{{REDACTED-email-address}}}",
                segment(filter(policy(null), header, TEXT), "headers and footers").getText());
        final BinaryDocumentFilterResult kept = filter(policy("{\"headersFooters\":\"keep\"}"), header, TEXT);
        Assertions.assertEquals(new TextSegment("headers and footers", "", TextSegment.Status.NOT_SCANNED),
                segment(kept, "headers and footers"));
        Assertions.assertFalse(kept.getText().join("\n").contains(EMAIL));

        final BinaryDocumentFilterResult all = filter(policy(KEEP_ALL), RtfDocs.everything(), TEXT);
        Assertions.assertEquals("Ask {{{REDACTED-email-address}}}", segment(all, "comments").getText());
        Assertions.assertEquals("Gone {{{REDACTED-email-address}}} ", segment(all, "tracked changes").getText());
        Assertions.assertTrue(segment(all, "body").getText().contains("hidden {{{REDACTED-email-address}}}"));

        // With the defaults, comments, deletions, and hidden text are removed, so they are not in the text.
        final BinaryDocumentFilterResult defaults = filter(policy(null), RtfDocs.everything(), TEXT);
        Assertions.assertEquals(List.of("body"), labels(defaults));
        Assertions.assertFalse(segment(defaults, "body").getText().contains("hidden"));
        Assertions.assertFalse(segment(defaults, "body").getText().contains("Gone"));

    }

    @Test
    public void theRtfOnlyTextOfEncapsulatedHtmlIsLeftOut() throws Exception {

        // As Outlook writes it: the HTML's text, with RTF-only text that stands in for markup in an RTF reader.
        final String rtf = "{\\rtf1\\ansi\\fromhtml1{\\fonttbl{\\f0 Arial;}}{\\*\\htmltag64 <p>}"
                + "\\htmlrtf {\\f0 rtf only " + OTHER_EMAIL + " }\\htmlrtf0 Mail " + EMAIL + "{\\*\\htmltag72 </p>}\\par}";

        final BinaryDocumentFilterResult text = filter(policy(null), rtf, TEXT);
        final BinaryDocumentFilterResult document = filter(policy(null), rtf, FilterOptions.defaults());

        Assertions.assertEquals("Mail {{{REDACTED-email-address}}}", segment(text, "body").getText());
        // The document redacts the RTF-only text too.
        Assertions.assertFalse(RtfDocs.text(document.getDocument()).contains(OTHER_EMAIL));

    }

    @Test
    public void fieldInstructionsAreNotIncluded() throws Exception {

        final BinaryDocumentFilterResult result = filter(policy(null),
                doc("{\\field{\\*\\fldinst HYPERLINK \"mailto:" + EMAIL + "\"}{\\fldrslt write us}}\\par "), TEXT);

        Assertions.assertEquals("write us", segment(result, "body").getText());

    }

    @Test
    public void detectsOnceForEachRunAsTheDocumentRedactionDoes() {

        final String rtf = doc("Email " + EMAIL + ".\\par Second.\\par{\\footnote\\pard Note.}\\par ");
        final Policy policy = policy(null);
        // Finds the address only, so the calls can be compared without the policy's filters.
        final Function<String, TextFilterResult> email = text -> {
            final int at = text.indexOf(EMAIL);
            if (at < 0) {
                return new TextFilterResult(text, "", 0, new Explanation(List.of(), List.of()), List.of(), 0);
            }
            final Span span = Span.make(at, at + EMAIL.length(), FilterType.EMAIL_ADDRESS, "", 1.0, EMAIL, "[email]",
                    "", false, true, new String[0], 0);
            return new TextFilterResult(text.replace(EMAIL, "[email]"), "", 0,
                    new Explanation(List.of(span), List.of(span)), List.of(), 0);
        };

        final List<String> textCalls = new ArrayList<>();
        RtfRedactor.redactToText(bytes(rtf), t -> { textCalls.add(t); return email.apply(t); }, policy.getConfig().getWord(), false);
        final List<String> documentCalls = new ArrayList<>();
        RtfRedactor.redact(bytes(rtf), t -> { documentCalls.add(t); return email.apply(t); }, policy.getConfig().getWord());

        Assertions.assertEquals(3, textCalls.size(), textCalls.toString());
        Assertions.assertEquals(documentCalls, textCalls);

    }

    @Test
    public void metadataFollowsTheDocumentPropertiesSetting() throws Exception {

        final String rtf = RtfDocs.withInfo("Body.\\par ").replace("{\\subject Planning}", "{\\subject Mail " + EMAIL + "}");

        Assertions.assertEquals(List.of("body"), labels(filter(policy(null), rtf, TEXT)));
        Assertions.assertEquals(new TextSegment("metadata", "", TextSegment.Status.NOT_SCANNED),
                segment(filter(policy(null), rtf, TEXT.withTextMetadata(true)), "metadata"));

        final String metadata = segment(filter(policy("{\"removeDocumentProperties\":false}"), rtf,
                TEXT.withTextMetadata(true)), "metadata").getText();
        Assertions.assertEquals("title: Quarterly notes\nsubject: Mail {{{REDACTED-email-address}}}\nkeywords: budget\n"
                + "category: Finance\ncomments: Draft for review", metadata);

    }

    @Test
    public void theLeftoverCheckRunsOverTheText() throws Exception {

        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}},\"config\":{\"review\":"
                + "{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"broad\"}}}}", Policy.class);
        final BinaryDocumentFilterResult result = filter(policy, doc("Mail " + EMAIL + ".\\par Call 212-555-0187.\\par "),
                TEXT.withLeftoverCheckMatchedText(true));

        final List<LeftoverFinding> findings = result.getLeftoverCheck().getFindings();
        Assertions.assertTrue(findings.stream().noneMatch(f -> f.getFilterType() == FilterType.EMAIL_ADDRESS), findings.toString());
        final LeftoverFinding phone = findings.stream().filter(f -> "212-555-0187".equals(f.getMatchedText())).findFirst().orElseThrow();
        Assertions.assertEquals("body", phone.getSegment());
        Assertions.assertEquals("212-555-0187", segment(result, "body").getText()
                .substring(phone.getCharacterStart(), phone.getCharacterEnd()));

    }

    @Test
    public void aValueInSeveralParagraphsGetsOneDocumentScopedReplacement() throws Exception {

        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{\"emailAddressFilterStrategies\":"
                + "[{\"strategy\":\"RANDOM_REPLACE\"}]}}}", Policy.class);

        final List<Span> spans = filter(policy, doc("Mail " + EMAIL + ".\\par Or " + EMAIL + ".\\par{\\footnote\\pard " + EMAIL + "}\\par "),
                FilterOptions.defaults()).getExplanation().appliedSpans();

        Assertions.assertEquals(3, spans.size());
        Assertions.assertEquals(1, spans.stream().map(Span::getReplacement).distinct().count(), spans.toString());

    }

    @Test
    public void theOutputTypeIsNotCheckedForText() throws Exception {
        Assertions.assertNotNull(SERVICE.filter(policy(null), new DefaultContextService(), new InMemoryVectorService(), "ctx",
                bytes(doc("Body.\\par ")), MimeType.TEXT_PLAIN, TEXT).getText());
    }

}
