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
package ai.philterd.phileas.services.csv;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvDocumentTest {

    private static CsvDocument parse(final String text) {
        return CsvDocument.parse(text, ",", "\"", CsvDocument.Limits.none());
    }

    private static List<List<String>> values(final CsvDocument document) {
        final List<List<String>> out = new ArrayList<>();
        for (final List<CsvDocument.Field> row : document.rows()) {
            out.add(row.stream().map(CsvDocument.Field::value).toList());
        }
        return out;
    }

    @Test
    public void readsQuotedFieldsWithDelimitersQuotesAndLineBreaks() {
        final CsvDocument document = parse("name,notes\n\"Doe, Jane\",\"said \"\"hi\"\"\nthen left\"\n");
        Assertions.assertEquals(List.of(List.of("name", "notes"), List.of("Doe, Jane", "said \"hi\"\nthen left")),
                values(document));
        Assertions.assertTrue(document.rows().get(1).get(0).quoted());
    }

    @ParameterizedTest
    @ValueSource(strings = {",", ";", "\t", "|"})
    public void readsEachDelimiter(final String delimiter) {
        final String text = "a" + delimiter + "b" + delimiter + "\"c" + delimiter + "d\"\r\n1" + delimiter + delimiter + "3";
        final CsvDocument document = CsvDocument.parse(text, delimiter, "\"", CsvDocument.Limits.none());
        Assertions.assertEquals(List.of(List.of("a", "b", "c" + delimiter + "d"), List.of("1", "", "3")), values(document));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    public void readsEachLineEnding(final String lineEnding) {
        final CsvDocument document = parse("a,b" + lineEnding + "c,d" + lineEnding);
        Assertions.assertEquals(List.of(List.of("a", "b"), List.of("c", "d")), values(document));
    }

    @Test
    public void readsEmptyFieldsBlankLinesAndRowsOfDifferingLengths() {
        final CsvDocument document = parse("a,,c\n\n,\nonly\n1,2,3,4");
        Assertions.assertEquals(List.of(List.of("a", "", "c"), List.of(""), List.of("", ""), List.of("only"),
                List.of("1", "2", "3", "4")), values(document));
    }

    @Test
    public void readsAnEmptyDocumentAsNoRows() {
        Assertions.assertTrue(parse("").rows().isEmpty());
    }

    @Test
    public void readsMalformedQuotingLeniently() {
        // Text after a closing quote is kept, a quote inside an unquoted field is literal, and an unclosed quote
        // runs to the end.
        final CsvDocument document = parse("\"ab\"cd,e\"f\n\"open,x\ny");
        Assertions.assertEquals(List.of(List.of("abcd", "e\"f"), List.of("open,x\ny")), values(document));
    }

    @Test
    public void writesBackOnlyTheFieldsGivenValues() {
        final String text = "id;\"name\";note\r\n7;\"Doe; Jane\";  keep  \r\n8;x;\"y\"";
        final CsvDocument document = CsvDocument.parse(text, ";", "\"", CsvDocument.Limits.none());

        Assertions.assertEquals(text, document.write(Map.of()));

        final CsvDocument.Field name = document.rows().get(1).get(1);
        final CsvDocument.Field plain = document.rows().get(2).get(1);
        final String written = document.write(Map.of(name, "R\"1", plain, "a;b"));
        Assertions.assertEquals("id;\"name\";note\r\n7;\"R\"\"1\";  keep  \r\n8;\"a;b\";\"y\"", written);
        Assertions.assertEquals(List.of(List.of("id", "name", "note"), List.of("7", "R\"1", "  keep  "),
                List.of("8", "a;b", "y")), values(CsvDocument.parse(written, ";", "\"", CsvDocument.Limits.none())));
    }

    @Test
    public void keepsWhetherTheDocumentEndsWithALineBreak() {
        for (final String text : List.of("a,b\n", "a,b", "a,b\r\n")) {
            final CsvDocument document = parse(text);
            Assertions.assertEquals(text.replace("a", "Z"), document.write(Map.of(document.rows().get(0).get(0), "Z")));
        }
    }

    @Test
    public void readsAnotherQuoteCharacter() {
        final CsvDocument document = CsvDocument.parse("'it''s',\"x\"", ",", "'", CsvDocument.Limits.none());
        Assertions.assertEquals(List.of(List.of("it's", "\"x\"")), values(document));
    }

    @Test
    public void enforcesTheLimits() {
        Assertions.assertThrows(CsvLimitExceededException.class,
                () -> CsvDocument.parse("a\nb\nc", ",", "\"", new CsvDocument.Limits(2, 0, 0)));
        Assertions.assertThrows(CsvLimitExceededException.class,
                () -> CsvDocument.parse("a,b,c", ",", "\"", new CsvDocument.Limits(0, 2, 0)));
        Assertions.assertThrows(CsvLimitExceededException.class,
                () -> CsvDocument.parse("\"abcdef\"", ",", "\"", new CsvDocument.Limits(0, 0, 5)));
        Assertions.assertEquals(3, CsvDocument.parse("a\nb\nc", ",", "\"", new CsvDocument.Limits(3, 1, 1)).rows().size());
    }

}
