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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A CSV document parsed into rows of fields, each with where its raw text is in the document. Writing it back
 * copies the document and rewrites only the fields given new values, so everything else, including each field's
 * quoting, the line endings, and whether the document ends with a line break, is kept as it was.
 *
 * <p>Fields are separated by the delimiter and records by a line break (CRLF, LF, or CR). A field that starts with
 * the quote character runs to the next quote not doubled; a doubled quote inside it is one quote, and the delimiter
 * and line breaks inside it are part of the value. Malformed quoting is read leniently: text after a closing quote
 * is kept in the value, and a quote that is never closed runs to the end of the document.</p>
 */
public final class CsvDocument {

    /**
     * A field: its raw text at {@code [start, end)} in the document, its value, and whether it was quoted.
     * @param start Where the raw text starts in the document.
     * @param end Where the raw text ends in the document.
     * @param value The value, with quoting removed.
     * @param quoted Whether the field was quoted.
     */
    public record Field(int start, int end, String value, boolean quoted) {
    }

    /** The limits on a document, each zero or less for none. */
    public record Limits(int maxRows, int maxFieldsPerRow, int maxFieldCharacters) {

        /** No limits. */
        public static Limits none() {
            return new Limits(0, 0, 0);
        }

    }

    private final String text;
    private final String delimiter;
    private final String quote;
    private final List<List<Field>> rows;

    private CsvDocument(final String text, final String delimiter, final String quote, final List<List<Field>> rows) {
        this.text = text;
        this.delimiter = delimiter;
        this.quote = quote;
        this.rows = rows;
    }

    /**
     * Parses a document.
     * @param text The document's text.
     * @param delimiter The field delimiter, one character.
     * @param quote The quote character, one character, different from the delimiter.
     * @param limits The limits.
     * @return The document.
     * @throws CsvLimitExceededException when the document exceeds a limit.
     */
    public static CsvDocument parse(final String text, final String delimiter, final String quote, final Limits limits) {

        if (delimiter.isEmpty() || quote.isEmpty() || delimiter.equals(quote)) {
            throw new IllegalArgumentException("The CSV delimiter and quote must be single, different characters.");
        }

        final List<List<Field>> rows = new ArrayList<>();
        final int length = text.length();
        int i = 0;

        while (i < length) {

            final List<Field> row = new ArrayList<>();

            while (true) {

                final int start = i;
                final StringBuilder value = new StringBuilder();
                boolean quoted = false;

                if (text.startsWith(quote, i)) {
                    quoted = true;
                    i += quote.length();
                    boolean open = true;
                    while (i < length && open) {
                        if (text.startsWith(quote, i)) {
                            if (text.startsWith(quote, i + quote.length())) {
                                value.append(quote);
                                i += 2 * quote.length();
                            } else {
                                i += quote.length();
                                open = false;
                            }
                        } else {
                            value.append(text.charAt(i));
                            i++;
                        }
                        checkFieldLength(value, limits);
                    }
                }

                // Unquoted text, or what follows a closing quote, up to the delimiter or a line break.
                while (i < length && !text.startsWith(delimiter, i) && text.charAt(i) != '\n' && text.charAt(i) != '\r') {
                    value.append(text.charAt(i));
                    i++;
                    checkFieldLength(value, limits);
                }

                row.add(new Field(start, i, value.toString(), quoted));
                if (limits.maxFieldsPerRow() > 0 && row.size() > limits.maxFieldsPerRow()) {
                    throw new CsvLimitExceededException("A row of the CSV document has more than "
                            + limits.maxFieldsPerRow() + " fields.");
                }

                if (i < length && text.startsWith(delimiter, i)) {
                    i += delimiter.length();
                    continue;
                }
                break;

            }

            rows.add(Collections.unmodifiableList(row));
            if (limits.maxRows() > 0 && rows.size() > limits.maxRows()) {
                throw new CsvLimitExceededException("The CSV document has more than " + limits.maxRows() + " rows.");
            }

            // The line break that ends the row.
            if (i < length && text.charAt(i) == '\r') {
                i++;
                if (i < length && text.charAt(i) == '\n') {
                    i++;
                }
            } else if (i < length && text.charAt(i) == '\n') {
                i++;
            }

        }

        return new CsvDocument(text, delimiter, quote, Collections.unmodifiableList(rows));

    }

    private static void checkFieldLength(final StringBuilder value, final Limits limits) {
        if (limits.maxFieldCharacters() > 0 && value.length() > limits.maxFieldCharacters()) {
            throw new CsvLimitExceededException("A field of the CSV document has more than "
                    + limits.maxFieldCharacters() + " characters.");
        }
    }

    /** The rows, each a list of its fields. */
    public List<List<Field>> rows() {
        return rows;
    }

    /** The document's text. */
    public String text() {
        return text;
    }

    /**
     * The document with some fields given new values. A new value is quoted when its field was quoted or when it
     * holds the delimiter, the quote, or a line break; a quote in it is doubled.
     * @param values The new values, by field.
     * @return The document's text.
     */
    public String write(final Map<Field, String> values) {
        final StringBuilder out = new StringBuilder(text.length());
        int copied = 0;
        for (final List<Field> row : rows) {
            for (final Field field : row) {
                final String value = values.get(field);
                if (value == null) {
                    continue;
                }
                out.append(text, copied, field.start());
                out.append(encode(field, value));
                copied = field.end();
            }
        }
        out.append(text, copied, text.length());
        return out.toString();
    }

    private String encode(final Field field, final String value) {
        if (field.quoted() || value.contains(delimiter) || value.contains(quote)
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return quote + value.replace(quote, quote + quote) + quote;
        }
        return value;
    }

}
