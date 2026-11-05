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
package ai.philterd.phileas.services.filters.regex;

/**
 * Regular expression fragments for the separators of hyphenated U.S. and Canadian identifiers (SSN,
 * ITIN, EIN, and Canadian SIN), shared so that each filter accepts exactly the same separators.
 */
public final class IdentifierSeparators {

    /**
     * The ASCII hyphen and its substitutes, for use inside a character class: the soft hyphen, the
     * U+2010 dash block (including the non-breaking hyphen), the minus sign, and the small and
     * fullwidth forms.
     */
    public static final String HYPHENS = "-\\u00AD\\u2010-\\u2015\\u2212\\uFE58\\uFE63\\uFF0D";

    /**
     * The hyphens that join digits into one longer token, for use inside a character class: the
     * ASCII hyphen, the soft hyphen, U+2010 and U+2011, the figure dash, the minus sign, and the small
     * and fullwidth hyphen-minus. An identifier may not begin or end next to one of these, as in
     * {@code 123-45-6789123-45-6789}. The en dash, em dash, horizontal bar, and small em dash are
     * left out: they are accepted between an identifier's groups, but next to one they are
     * punctuation, such as an em dash (U+2014) after an identifier in a sentence, or an en dash (U+2013)
     * joining a range of two identifiers.
     */
    public static final String JOINING_HYPHENS = "-\\u00AD\\u2010-\\u2012\\u2212\\uFE63\\uFF0D";

    /**
     * A hyphen the identifier may be wrapped across. Requiring the hyphen before the line break keeps
     * numbers on separate lines from matching. Whitespace is never part of a digit group, so the runs
     * are possessive.
     */
    public static final String WRAP = "[" + HYPHENS + "]\\h*+(?:\\R\\h*+)?+";

    /**
     * Joins an identifier's three digit groups. Both separators are the same kind: a hyphen, wrapped
     * or not, one horizontal space, or nothing. Allowing each to be chosen on its own let a ZIP+4 such
     * as {@code 52168-1208} match as an SSN, with nothing after the first group and a hyphen after the
     * second. See issue #460.
     */
    public static String join(final String first, final String second, final String third) {
        return first + "(?:" + WRAP + second + WRAP + "|\\h" + second + "\\h|" + second + ")" + third;
    }

    private IdentifierSeparators() {
        // Constants only.
    }

}
