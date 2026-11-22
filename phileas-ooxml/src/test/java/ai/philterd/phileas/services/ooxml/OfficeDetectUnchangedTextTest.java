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

import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Detect reports a paragraph's or cell's spans only when the filter changed its text, as redact does, so
 * the two return the same spans when a span's replacement is the original text. See
 * philterd/phileas-dotnet#155.
 */
public class OfficeDetectUnchangedTextTest {

    private static String describe(final OfficeRedactionSpan s) {
        return s.getOrder() + "|" + s.getParagraphIndex() + "|" + s.getCharacterStart() + "-" + s.getCharacterEnd()
                + "|" + s.getText() + "|" + s.getReplacement();
    }

    // Reports the address without replacing it: the filtered text is the input.
    private static TextFilterResult reportWithoutReplacing(final String text) {
        final int i = text.indexOf("a@example.com");
        if (i < 0) {
            return TestFilters.result(text, List.of());
        }
        final Span span = TestFilters.span(text, i, i + 13, "a@example.com");
        return TestFilters.result(text, List.of(span));
    }

    // Java Phileas has no SAME strategy (phileas-dotnet sets it in code); a static replacement equal to the
    // value leaves the text unchanged the same way.
    private static TextFilter sameSsnFilter(final boolean redactEmails) {
        return TestFilters.forPolicy("{\"identifiers\":{"
                + "\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"STATIC_REPLACE\",\"staticReplacement\":\"123-45-6789\"}]}"
                + (redactEmails ? ",\"emailAddress\":{}" : "") + "}}");
    }

    private static OoxmlRedactionOptions defaults() {
        return OoxmlRedactionOptions.defaults();
    }

    @Test
    public void wordASpanThatLeavesTheTextUnchangedIsReportedByNeither() throws Exception {
        final byte[] input = WordFixturesForEmbeddedTests.create("Mail a@example.com now");

        final List<OfficeRedactionSpan> detected = WordDocumentRedactor.detect(input, OfficeDetectUnchangedTextTest::reportWithoutReplacing, defaults());
        final List<OfficeRedactionSpan> redacted = WordDocumentRedactor.redact(input, OfficeDetectUnchangedTextTest::reportWithoutReplacing, defaults()).spans();

        Assertions.assertTrue(detected.isEmpty());
        Assertions.assertTrue(redacted.isEmpty());
    }

    @Test
    public void wordASameStrategyIsReportedByNeither() throws Exception {
        final byte[] input = WordFixturesForEmbeddedTests.create("ssn 123-45-6789 on file");

        Assertions.assertTrue(WordDocumentRedactor.detect(input, sameSsnFilter(false), defaults()).isEmpty());
        Assertions.assertTrue(WordDocumentRedactor.redact(input, sameSsnFilter(false), defaults()).spans().isEmpty());
    }

    @Test
    public void wordAParagraphTheFilterChangesIsReportedWithEverySpanByBoth() throws Exception {
        // The unchanged span sits in a paragraph that another span changes, so both methods report both.
        final byte[] input = WordFixturesForEmbeddedTests.create("ssn 123-45-6789 and mail b@example.com");

        final List<OfficeRedactionSpan> detected = WordDocumentRedactor.detect(input, sameSsnFilter(true), defaults());
        final List<OfficeRedactionSpan> redacted = WordDocumentRedactor.redact(input, sameSsnFilter(true), defaults()).spans();

        Assertions.assertEquals(2, detected.size());
        Assertions.assertEquals(redacted.stream().map(OfficeDetectUnchangedTextTest::describe).toList(),
                detected.stream().map(OfficeDetectUnchangedTextTest::describe).toList());
    }

    @Test
    public void excelASpanThatLeavesTheTextUnchangedIsReportedByNeither() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsx(List.<String[]>of(new String[]{"Mail a@example.com now"}));

        Assertions.assertTrue(XlsxRedactor.detect(input, OfficeDetectUnchangedTextTest::reportWithoutReplacing, defaults()).isEmpty());
        Assertions.assertTrue(XlsxRedactor.redact(input, OfficeDetectUnchangedTextTest::reportWithoutReplacing, defaults()).spans().isEmpty());
    }

    @Test
    public void excelACellTheFilterChangesIsReportedWithEverySpanByBoth() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsx(List.<String[]>of(new String[]{"ssn 123-45-6789 and mail b@example.com"}));

        final List<OfficeRedactionSpan> detected = XlsxRedactor.detect(input, sameSsnFilter(true), defaults());
        final List<OfficeRedactionSpan> redacted = XlsxRedactor.redact(input, sameSsnFilter(true), defaults()).spans();

        Assertions.assertEquals(2, detected.size());
        Assertions.assertEquals(redacted.stream().map(OfficeDetectUnchangedTextTest::describe).toList(),
                detected.stream().map(OfficeDetectUnchangedTextTest::describe).toList());
    }

    @Test
    public void excelASameStrategyIsReportedByNeither() throws Exception {
        final byte[] input = SpreadsheetDocs.createXlsx(List.<String[]>of(new String[]{"ssn 123-45-6789 on file"}));

        Assertions.assertTrue(XlsxRedactor.detect(input, sameSsnFilter(false), defaults()).isEmpty());
        Assertions.assertTrue(XlsxRedactor.redact(input, sameSsnFilter(false), defaults()).spans().isEmpty());
    }

}
