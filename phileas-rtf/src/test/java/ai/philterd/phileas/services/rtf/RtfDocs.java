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

import java.nio.charset.StandardCharsets;

/** Generated RTF fixtures for tests. No real documents. */
final class RtfDocs {

    static final String EMAIL = "jane.doe@example.com";
    static final String OTHER_EMAIL = "john.roe@example.com";
    static final String SSN = "123-45-6789";
    static final String AUTHOR = "Example Author";
    static final String EDITOR = "Example Editor";
    static final String COMPANY = "Example Company";

    private static final String HEAD = "{\\rtf1\\ansi\\ansicpg1252\\deff0{\\fonttbl{\\f0\\fswiss Arial;}}"
            + "{\\colortbl;\\red0\\green0\\blue0;}\r\n";

    private RtfDocs() {
    }

    /** A document whose body is the given RTF. */
    static String doc(final String body) {
        return HEAD + "\\pard\\f0\\fs20 " + body + "}";
    }

    /** A document with document properties and the given body. */
    static String withInfo(final String body) {
        return HEAD + "{\\info{\\title Quarterly notes}{\\subject Planning}{\\keywords budget}{\\doccomm Draft for review}"
                + "{\\category Finance}{\\author " + AUTHOR + "}{\\operator " + EDITOR + "}{\\company " + COMPANY + "}"
                + "{\\manager " + AUTHOR + "}{\\*\\userprops {\\propname Client}\\proptype30{\\staticval Example Client}}}"
                + "\\pard\\f0\\fs20 " + body + "}";
    }

    /** A document with a comment on its body text. */
    static String commented() {
        return doc("Body text{\\*\\atrfstart 1}{\\*\\atrfend 1}{\\*\\atnid EA}{\\*\\atnauthor " + AUTHOR + "}\\chatn "
                + "{\\*\\annotation{\\*\\atnref 1}\\pard Ask " + EMAIL + "}.\\par ");
    }

    /** A document with a deletion, an insertion, and a revision table. */
    static String tracked() {
        return HEAD.replace("}\r\n", "}{\\*\\revtbl {Unknown;}{" + AUTHOR + ";}}\r\n")
                + "\\pard\\f0\\fs20 Kept {\\deleted\\revauthdel1 Gone " + OTHER_EMAIL + " }"
                + "{\\revised\\revauth1 Added " + EMAIL + "}.\\par }";
    }

    /**
     * A document with every place a name can be: document properties, a revision table, a comment, a deletion, an
     * insertion, and hidden text.
     */
    static String everything() {
        return withInfo("Body text{\\*\\atnid EA}{\\*\\atnauthor " + AUTHOR + "}\\chatn "
                + "{\\*\\annotation\\pard Ask " + EMAIL + "} {\\deleted\\revauthdel1 Gone " + OTHER_EMAIL + " }"
                + "{\\revised\\revauth1 Added " + EMAIL + "} {\\v hidden " + EMAIL + "}.\\par ")
                .replace("{\\info", "{\\*\\revtbl {Unknown;}{" + AUTHOR + ";}}{\\info");
    }

    static byte[] bytes(final String rtf) {
        return rtf.getBytes(StandardCharsets.ISO_8859_1);
    }

    static String text(final byte[] rtf) {
        return new String(rtf, StandardCharsets.ISO_8859_1);
    }

}
