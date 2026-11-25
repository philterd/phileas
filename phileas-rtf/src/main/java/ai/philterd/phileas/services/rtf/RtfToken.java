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

/**
 * One token of an RTF document, with its raw text as written, so a document is written back exactly by joining
 * its tokens' raw text.
 * @param kind The kind of token.
 * @param raw The raw text, one character per byte.
 * @param word A control word's name, or {@code null}.
 * @param parameter A control word's numeric parameter, or {@code null} when it has none.
 */
record RtfToken(Kind kind, String raw, String word, Integer parameter) {

    enum Kind {
        /** {@code {} */
        GROUP_START,
        /** {@code }} */
        GROUP_END,
        /** A control word, such as {@code \par} or {@code \fs20}, with its space delimiter if it had one. */
        WORD,
        /** A control symbol, such as {@code \~} or {@code \{}. */
        SYMBOL,
        /** A hex escape, {@code \'hh}. */
        HEX,
        /** Text. */
        TEXT,
        /** A line break, which RTF ignores. */
        NEWLINE,
        /** The binary data after {@code \binN}. */
        BINARY,
        /** Whatever follows the document's closing brace, kept as it is. */
        TRAILER
    }

    static RtfToken of(final Kind kind, final String raw) {
        return new RtfToken(kind, raw, null, null);
    }

    boolean is(final Kind kind) {
        return this.kind == kind;
    }

    boolean isWord(final String name) {
        return kind == Kind.WORD && name.equals(word);
    }

    /** A control word that ends without a delimiter, so text written right after it could join it. */
    boolean needsDelimiter() {
        if (kind != Kind.WORD) {
            return false;
        }
        final char last = raw.charAt(raw.length() - 1);
        return last != ' ';
    }

    /** A hex escape's byte. */
    int hexByte() {
        return Integer.parseInt(raw.substring(2, 4), 16);
    }

}
