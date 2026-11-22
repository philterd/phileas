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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Excel cell comments, legacy (xl/comments*.xml) and threaded (xl/threadedComments, with authors in
 * xl/persons), carry free text outside any cell. Their PII must be redacted.
 */
public class XlsxCommentRedactionTest {

    private static final TextFilter FILTER = TestFilters.forPolicy("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}}");

    private static final List<String[]> GRID = List.of(new String[]{"Name"}, new String[]{"Alice"});

    private static OoxmlRedactionOptions defaults() {
        return OoxmlRedactionOptions.defaults();
    }

    @Test
    public void redactLegacyCommentRedactsCommentTextKeepsTheComment() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithLegacyComment(GRID, "A1", "Follow up with john@example.com re SSN 123-45-6789.");

        final byte[] output = XlsxRedactor.redact(input, FILTER, defaults()).document();

        final String comments = SpreadsheetDocs.allCommentText(output);
        Assertions.assertFalse(comments.isEmpty());
        Assertions.assertFalse(comments.contains("john@example.com"));
        Assertions.assertFalse(comments.contains("123-45-6789"));
        Assertions.assertTrue(comments.contains("REDACTED"));
        Assertions.assertFalse(SpreadsheetDocs.allCommentXml(output).contains("john@example.com"));
    }

    @Test
    public void redactThreadedCommentRedactsTextAndAuthorPii() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithThreadedComment(GRID, "A1", "Please verify john@example.com.", "mary@example.com");

        final byte[] output = XlsxRedactor.redact(input, FILTER, defaults()).document();

        final String xml = SpreadsheetDocs.allCommentXml(output);
        Assertions.assertFalse(xml.contains("john@example.com"));
        Assertions.assertFalse(xml.contains("mary@example.com"));
    }

    @Test
    public void detectFindsPiiInComment() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithLegacyComment(GRID, "A1", "Reach john@example.com.");

        final List<OfficeRedactionSpan> residuals = XlsxRedactor.detect(input, FILTER, defaults());

        Assertions.assertTrue(residuals.stream().anyMatch(s -> s.getText().contains("john@example.com")));
    }

    @Test
    public void applySpansWithFilterRedactsCommentLikeModify() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithLegacyComment(GRID, "A1", "Old note: john@example.com.");

        // Modify re-renders from the source, passing the policy filter for the non-cell passes.
        final byte[] output = XlsxRedactor.applySpans(input, List.of(), defaults(), FILTER);

        Assertions.assertFalse(SpreadsheetDocs.allCommentXml(output).contains("john@example.com"));
    }

    @Test
    public void redactNonPiiCommentIsUnchanged() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsxWithLegacyComment(GRID, "A1", "Looks good to me.");

        final byte[] output = XlsxRedactor.redact(input, FILTER, defaults()).document();

        Assertions.assertTrue(SpreadsheetDocs.allCommentText(output).contains("Looks good to me."));
    }

}
