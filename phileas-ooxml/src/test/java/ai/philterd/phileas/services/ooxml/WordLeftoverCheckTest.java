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
package ai.philterd.phileas.services.ooxml;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import ai.philterd.phileas.services.filters.filtering.OoxmlFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

/**
 * The leftover check numbers a Word document's paragraphs as the redaction did, when the policy's Word settings
 * remove or skip text numbered before the paragraph checked.
 */
public class WordLeftoverCheckTest {

    static final String EMAIL = "jane.doe@example.com";
    static final String OTHER_EMAIL = "john.roe@example.com";
    static final String PHONE = "212-555-0187";

    // The paragraph checked: a random stand-in for the address, then a phone number the policy leaves.
    static final String TARGET = "Mail " + EMAIL + " or call " + PHONE + ".";

    static BinaryDocumentFilterResult filter(final String config, final byte[] input, final MimeType type) throws Exception {
        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":"
                + "{\"emailAddressFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]}},\"config\":{"
                + (config == null ? "" : config + ",")
                + "\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"broad\"}}}}", Policy.class);
        return new OoxmlFilterService(new PhileasConfiguration(new Properties()), null).filter(policy,
                new DefaultContextService(), new InMemoryVectorService(), "ctx", input, type,
                FilterOptions.defaults().withLeftoverCheckMatchedText(true));
    }

    /**
     * Asserts that no stand-in is reported, and that the phone number is reported in the paragraph or cell where
     * the redaction put the stand-in for {@link #EMAIL}, after that stand-in and any earlier stand-ins there.
     */
    static void assertLinedUp(final String label, final BinaryDocumentFilterResult result) {

        final List<LeftoverFinding> findings = result.getLeftoverCheck().getFindings();
        Assertions.assertTrue(result.getLeftoverCheck().isPerformed(), label);
        Assertions.assertTrue(findings.stream().noneMatch(f -> f.getFilterType() == FilterType.EMAIL_ADDRESS),
                label + " " + findings);

        final Span email = result.getExplanation().appliedSpans().stream()
                .filter(span -> EMAIL.equals(span.getText())).findFirst().orElseThrow(() -> new AssertionError(label));
        final LeftoverFinding phone = findings.stream().filter(f -> PHONE.equals(f.getMatchedText())).findFirst()
                .orElseThrow(() -> new AssertionError(label + " " + findings));

        // Where the stand-in for EMAIL starts in the output, after any earlier stand-ins in its paragraph.
        int shift = 0;
        for (final Span span : result.getExplanation().appliedSpans()) {
            if (span.getParagraphNumber() == email.getParagraphNumber() && span.getCharacterStart() < email.getCharacterStart()) {
                shift += span.getReplacement().length() - (span.getCharacterEnd() - span.getCharacterStart());
            }
        }
        final int start = email.getCharacterStart() + shift + email.getReplacement().length() + " or call ".length();
        Assertions.assertEquals(email.getParagraphNumber(), phone.getParagraphNumber(), label);
        Assertions.assertEquals(start, phone.getCharacterStart(), label);
        Assertions.assertEquals(start + PHONE.length(), phone.getCharacterEnd(), label);

    }

    @Test
    public void afterTextTheWordSettingsRemoveOrSkip() throws Exception {

        final byte[] input = WordDocs.createWithEverythingBeforeAnEndnote(OTHER_EMAIL, TARGET);

        // Hidden text and the comment removed, the deletion accepted, headers and footers redacted.
        assertLinedUp("defaults", filter(null, input, MimeType.APPLICATION_DOCX));
        assertLinedUp("headers and footers kept", filter("\"word\":{\"headersFooters\":\"keep\"}", input, MimeType.APPLICATION_DOCX));

        final BinaryDocumentFilterResult kept = filter("\"word\":{\"hiddenText\":\"redact\",\"comments\":\"redact\","
                + "\"trackedChanges\":\"redact\"}", input, MimeType.APPLICATION_DOCX);
        assertLinedUp("everything kept and redacted", kept);

        // The hidden text, header, footer, and footnote are numbered before the endnote, as is the tracked
        // deletion's paragraph (its span has no paragraph), so the settings above change what precedes the
        // paragraph checked. The comment is numbered after it.
        final int target = kept.getExplanation().appliedSpans().stream().filter(span -> EMAIL.equals(span.getText()))
                .findFirst().orElseThrow().getParagraphNumber();
        Assertions.assertEquals(4, kept.getExplanation().appliedSpans().stream()
                .filter(span -> OTHER_EMAIL.equals(span.getText()))
                .filter(span -> span.getParagraphNumber() >= 0 && span.getParagraphNumber() < target).count());

    }

    @Test
    public void afterHiddenTextInTheSameParagraph() throws Exception {
        final byte[] input = WordDocs.createRaw("<w:p><w:r><w:rPr><w:vanish/></w:rPr><w:t xml:space=\"preserve\">Secret "
                + OTHER_EMAIL + " </w:t></w:r><w:r><w:t xml:space=\"preserve\">" + TARGET + "</w:t></w:r></w:p>");
        assertLinedUp("removed", filter(null, input, MimeType.APPLICATION_DOCX));
        assertLinedUp("redacted", filter("\"word\":{\"hiddenText\":\"redact\"}", input, MimeType.APPLICATION_DOCX));
    }

}
