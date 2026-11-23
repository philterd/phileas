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
 * The byte[] API of the Word and Excel redactors: that it redacts, returns spans, round-trips through
 * applySpans, leaves the input array untouched, and gives the same result on repeated calls. (phileas-dotnet
 * also has file-path overloads; Java has only byte[], so the "matches path overload" tests check that two
 * calls on the same bytes agree.)
 */
public class OfficeByteOverloadTest {

    private static TextFilter filter() {
        return TestFilters.forPolicy("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}}");
    }

    private static OoxmlRedactionOptions defaults() {
        return OoxmlRedactionOptions.defaults();
    }

    // --- Word -----------------------------------------------------------------------------------

    @Test
    public void wordRedactBytesRedactsAndReturnsSpans() throws Exception {
        final byte[] input = WordFixturesForEmbeddedTests.create("keep this", "email alice@example.com here");

        final OoxmlRedactionResult result = WordDocumentRedactor.redact(input, filter(), defaults());

        Assertions.assertFalse(result.spans().isEmpty());
        Assertions.assertTrue(result.spans().stream().anyMatch(s -> "alice@example.com".equals(s.getText())));
        final String body = WordFixturesForEmbeddedTests.allBodyText(result.document());
        Assertions.assertFalse(body.contains("alice@example.com"));
        Assertions.assertTrue(body.contains("keep this"));
    }

    @Test
    public void wordRedactBytesDoesNotMutateInputArray() throws Exception {
        final byte[] input = WordFixturesForEmbeddedTests.create("email alice@example.com here");
        final byte[] snapshot = input.clone();

        WordDocumentRedactor.redact(input, filter(), defaults());

        Assertions.assertArrayEquals(snapshot, input);
    }

    @Test
    public void wordRedactBytesIsRepeatable() throws Exception {
        final byte[] input = WordFixturesForEmbeddedTests.create("keep", "email bob@example.com and ssn 123-45-6789");

        final OoxmlRedactionResult first = WordDocumentRedactor.redact(input, filter(), defaults());
        final OoxmlRedactionResult second = WordDocumentRedactor.redact(input, filter(), defaults());

        Assertions.assertEquals(first.spans().size(), second.spans().size());
        Assertions.assertEquals(WordFixturesForEmbeddedTests.allBodyText(first.document()),
                WordFixturesForEmbeddedTests.allBodyText(second.document()));
    }

    @Test
    public void wordDetectBytesIsRepeatable() throws Exception {
        final byte[] input = WordFixturesForEmbeddedTests.create("email carol@example.com", "ssn 123-45-6789");

        final List<OfficeRedactionSpan> first = WordDocumentRedactor.detect(input, filter(), defaults());
        final List<OfficeRedactionSpan> second = WordDocumentRedactor.detect(input, filter(), defaults());

        Assertions.assertEquals(first.size(), second.size());
        Assertions.assertTrue(second.stream().anyMatch(s -> "carol@example.com".equals(s.getText())));
    }

    @Test
    public void wordApplySpansBytesRoundTrips() throws Exception {
        final byte[] input = WordFixturesForEmbeddedTests.create("call dave@example.com");

        final List<OfficeRedactionSpan> spans = WordDocumentRedactor.detect(input, filter(), defaults());
        final byte[] document = WordDocumentRedactor.applySpans(input, spans, defaults().setHighlight(false), null);

        Assertions.assertFalse(WordFixturesForEmbeddedTests.allBodyText(document).contains("dave@example.com"));
    }

    @Test
    public void wordReadParagraphsAndReviewLinesBytesAreRepeatable() throws Exception {
        final byte[] input = WordFixturesForEmbeddedTests.create("alpha", "beta", "gamma");

        Assertions.assertEquals(List.of("alpha", "beta", "gamma"), WordDocumentRedactor.readParagraphs(input));
        Assertions.assertEquals(WordDocumentRedactor.readParagraphs(input), WordDocumentRedactor.readParagraphs(input));
        Assertions.assertEquals(WordDocumentRedactor.readReviewLines(input), WordDocumentRedactor.readReviewLines(input));
    }

    // --- Excel ----------------------------------------------------------------------------------

    private static final List<String[]> ROWS = List.of(
            new String[]{"Name", "Email", "Notes"},
            new String[]{"Alice", "alice@example.com", "VIP"},
            new String[]{"Bob", "bob@example.com", "ssn 123-45-6789"});

    @Test
    public void xlsxRedactBytesRedactsAndReturnsSpans() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsx(ROWS);

        final OoxmlRedactionResult result = XlsxRedactor.redact(input, filter(), defaults());

        Assertions.assertFalse(result.spans().isEmpty());
        final String text = SpreadsheetDocs.allText(result.document());
        Assertions.assertFalse(text.contains("alice@example.com"));
        Assertions.assertFalse(text.contains("bob@example.com"));
        Assertions.assertFalse(text.contains("123-45-6789"));
        Assertions.assertTrue(text.contains("VIP"));
    }

    @Test
    public void xlsxRedactBytesDoesNotMutateInputArray() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsx(ROWS);
        final byte[] snapshot = input.clone();

        XlsxRedactor.redact(input, filter(), defaults());

        Assertions.assertArrayEquals(snapshot, input);
    }

    @Test
    public void xlsxRedactBytesIsRepeatable() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsx(ROWS);

        final OoxmlRedactionResult first = XlsxRedactor.redact(input, filter(), defaults());
        final OoxmlRedactionResult second = XlsxRedactor.redact(input, filter(), defaults());

        Assertions.assertEquals(first.spans().size(), second.spans().size());
        Assertions.assertEquals(SpreadsheetDocs.allText(first.document()), SpreadsheetDocs.allText(second.document()));
    }

    @Test
    public void xlsxDetectBytesIsRepeatable() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsx(ROWS);

        final List<OfficeRedactionSpan> first = XlsxRedactor.detect(input, filter(), defaults());
        final List<OfficeRedactionSpan> second = XlsxRedactor.detect(input, filter(), defaults());

        Assertions.assertEquals(first.size(), second.size());
    }

    @Test
    public void xlsxApplySpansBytesRoundTrips() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsx(ROWS);

        final List<OfficeRedactionSpan> spans = XlsxRedactor.redact(input, filter(), defaults()).spans();
        final byte[] document = XlsxRedactor.applySpans(input, spans, defaults(), null);

        Assertions.assertFalse(SpreadsheetDocs.allText(document).contains("alice@example.com"));
    }

    @Test
    public void xlsxReadSheetNamesAndReadColumnsBytesAreRepeatable() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsx(ROWS);

        Assertions.assertEquals(List.of("Sheet1"), XlsxRedactor.readSheetNames(input));
        Assertions.assertEquals(XlsxRedactor.readSheetNames(input), XlsxRedactor.readSheetNames(input));

        final List<SpreadsheetColumn> first = XlsxRedactor.readColumns(input, null);
        final List<SpreadsheetColumn> second = XlsxRedactor.readColumns(input, null);
        Assertions.assertEquals(first.size(), second.size());
        Assertions.assertEquals(first.stream().map(SpreadsheetColumn::header).toList(),
                second.stream().map(SpreadsheetColumn::header).toList());
        Assertions.assertEquals(List.of("Name", "Email", "Notes"), first.stream().map(SpreadsheetColumn::header).toList());
    }

}
