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
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.LeftoverCheckResult;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.LeftoverCheck;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import ai.philterd.phileas.services.filters.filtering.RtfFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static ai.philterd.phileas.services.rtf.RtfDocs.EMAIL;
import static ai.philterd.phileas.services.rtf.RtfDocs.SSN;
import static ai.philterd.phileas.services.rtf.RtfDocs.bytes;
import static ai.philterd.phileas.services.rtf.RtfDocs.doc;
import static ai.philterd.phileas.services.rtf.RtfDocs.text;

public class RtfLeftoverCheckTest {

    private static final String PHONE = "212-555-0187";

    private static final FilterOptions REQUESTED = FilterOptions.defaults().withLeftoverCheck(true)
            .withLeftoverCheckMatchedText(true);

    private static final RtfFilterService SERVICE = new RtfFilterService(new PhileasConfiguration(new Properties()),
            new DefaultContextService(), new InMemoryVectorService(), null);

    // Email addresses get random stand-ins, which look like real addresses.
    private static Policy policy(final String using, final String word) {
        return new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{\"emailAddressFilterStrategies\":"
                + "[{\"strategy\":\"RANDOM_REPLACE\"}]}},\"config\":{"
                + (word == null ? "" : "\"word\":" + word + ",")
                + "\"review\":{\"leftoverCheck\":{\"mode\":\"onRequest\",\"using\":\"" + using + "\"}}}}", Policy.class);
    }

    private static BinaryDocumentFilterResult filter(final Policy policy, final String rtf, final FilterOptions options)
            throws Exception {
        return SERVICE.filter(policy, "ctx", bytes(rtf), MimeType.APPLICATION_RTF, options);
    }

    @Test
    public void findsWhatIsLeftInItsParagraphAndNotTheReplacements() throws Exception {

        final String input = doc("Keep this.\\par Email " + EMAIL + " now.\\par Call " + PHONE + " or mail " + EMAIL
                + ", SSN " + SSN + ".\\par ");

        final BinaryDocumentFilterResult result = filter(policy("broad", null), input, REQUESTED);
        final String output = text(result.getDocument());
        Assertions.assertFalse(output.contains(EMAIL), output);

        final LeftoverCheckResult check = result.getLeftoverCheck();
        Assertions.assertTrue(check.isPerformed());

        // The random stand-ins for the email addresses are not reported.
        Assertions.assertTrue(check.getFindings().stream().noneMatch(f -> f.getFilterType() == FilterType.EMAIL_ADDRESS),
                check.getFindings().toString());

        final List<String> paragraphs = RtfRedactor.readParagraphs(result.getDocument(), policy("broad", null).getConfig().getWord());

        for (final String value : List.of(PHONE, SSN)) {
            final LeftoverFinding finding = check.getFindings().stream()
                    .filter(f -> value.equals(f.getMatchedText())).findFirst().orElseThrow(() -> new AssertionError(value));
            Assertions.assertEquals(2, finding.getParagraphNumber(), value);
            Assertions.assertEquals(value, paragraphs.get(2).substring(finding.getCharacterStart(), finding.getCharacterEnd()));
        }

    }

    @Test
    public void samePolicyDoesNotReportATypeThePolicyDoesNotInclude() throws Exception {
        final BinaryDocumentFilterResult result = filter(policy("samePolicy", null),
                doc("Email " + EMAIL + ", SSN " + SSN + ".\\par "), REQUESTED);
        Assertions.assertTrue(result.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(0, result.getLeftoverCheck().getFindingCount(), result.getLeftoverCheck().getFindings().toString());
    }

    @Test
    public void paragraphsLineUpWhenTheWordSettingsRemoveText() throws Exception {

        // A removed comment and an accepted deletion are gone from the output, so the check must number the
        // output's paragraphs as the redaction numbered the input's, or a stand-in lands in the wrong place.
        final String input = doc("Note{\\*\\atnid A}{\\*\\atnauthor Example Author}\\chatn "
                + "{\\*\\annotation\\pard Ask " + EMAIL + "}.\\par "
                + "Kept {\\deleted Gone " + EMAIL + " }.\\par "
                + "Email " + EMAIL + " and SSN " + SSN + ".\\par ");

        final Policy policy = policy("broad", "{\"comments\":\"remove\",\"trackedChanges\":\"accept\"}");
        final BinaryDocumentFilterResult result = filter(policy, input, REQUESTED);

        final List<LeftoverFinding> findings = result.getLeftoverCheck().getFindings();
        Assertions.assertTrue(findings.stream().noneMatch(f -> f.getFilterType() == FilterType.EMAIL_ADDRESS), findings.toString());

        final LeftoverFinding ssn = findings.stream().filter(f -> f.getFilterType() == FilterType.SSN).findFirst().orElseThrow();
        final List<String> paragraphs = RtfRedactor.readParagraphs(result.getDocument(), policy.getConfig().getWord());
        Assertions.assertEquals(SSN, paragraphs.get(ssn.getParagraphNumber()).substring(ssn.getCharacterStart(), ssn.getCharacterEnd()));

    }

    @Test
    public void headersAndFootersKeptAreNotChecked() throws Exception {
        final String input = doc("{\\header\\pard Header " + SSN + "\\par}Body " + PHONE + ".\\par ");

        final List<LeftoverFinding> kept = filter(policy("broad", "{\"headersFooters\":\"keep\"}"), input, REQUESTED)
                .getLeftoverCheck().getFindings();
        Assertions.assertTrue(kept.stream().noneMatch(f -> f.getFilterType() == FilterType.SSN), kept.toString());
        Assertions.assertTrue(kept.stream().anyMatch(f -> PHONE.equals(f.getMatchedText())), kept.toString());

        final List<LeftoverFinding> redacted = filter(policy("broad", "{\"headersFooters\":\"redact\"}"), input, REQUESTED)
                .getLeftoverCheck().getFindings();
        Assertions.assertTrue(redacted.stream().anyMatch(f -> SSN.equals(f.getMatchedText())), redacted.toString());
    }

    @Test
    public void aCheckThatFailsIsReportedAsNotPerformed() throws Exception {
        final RtfFilterService failing = new RtfFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null) {
            @Override
            protected List<Span> leftoverSpans(final Policy p, final LeftoverCheck.Using using, final String text) {
                throw new IllegalStateException("check failed");
            }
        };
        // Redaction labels rather than random stand-ins, so the output can be compared byte for byte.
        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}},\"config\":{\"review\":"
                + "{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"broad\"}}}}", Policy.class);
        final String input = doc("Email " + EMAIL + ", SSN " + SSN + ".\\par ");

        final BinaryDocumentFilterResult result = failing.filter(policy, "ctx", bytes(input), MimeType.APPLICATION_RTF, REQUESTED);

        Assertions.assertArrayEquals(SERVICE.filter(policy, "ctx", bytes(input), MimeType.APPLICATION_RTF).getDocument(),
                result.getDocument());
        Assertions.assertFalse(result.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(LeftoverCheckResult.Reason.CHECK_FAILED, result.getLeftoverCheck().getReason());
    }

    @Test
    public void notRunUnlessRequestedAndDoesNotChangeTheOutput() throws Exception {

        final String input = doc("Email " + EMAIL + ", SSN " + SSN + ".\\par ");
        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}},\"config\":{\"review\":"
                + "{\"leftoverCheck\":{\"mode\":\"onRequest\",\"using\":\"broad\"}}}}", Policy.class);

        final BinaryDocumentFilterResult unchecked = filter(policy, input, FilterOptions.defaults());
        Assertions.assertFalse(unchecked.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(LeftoverCheckResult.Reason.NOT_REQUESTED, unchecked.getLeftoverCheck().getReason());

        final BinaryDocumentFilterResult checked = filter(policy, input, FilterOptions.defaults().withLeftoverCheck(true));
        Assertions.assertTrue(checked.getLeftoverCheck().getFindingCount() > 0);
        Assertions.assertArrayEquals(unchecked.getDocument(), checked.getDocument());
        Assertions.assertEquals(unchecked.getExplanation(), checked.getExplanation());

        // Matched text is held only when asked for.
        checked.getLeftoverCheck().getFindings().forEach(f -> Assertions.assertNull(f.getMatchedText()));

    }

}
