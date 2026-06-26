/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates International Bank Account Numbers (IBANs) per ISO 13616, in-tree so Phileas
 * does not depend on commons-validator (which pulled in four further transitive jars).
 *
 * <p>An IBAN is valid when its first two characters are a known country code, the whole
 * string matches that country's fixed structure (length and per-position character classes),
 * and the ISO 7064 MOD 97-10 checksum over the rearranged value equals 1.</p>
 *
 * <p>The country structure table mirrors the IBAN registry that commons-validator 1.10.0
 * shipped, so validation results are identical to the previous implementation. Matching is
 * case-sensitive (IBAN letters are upper-case); a lower-case value is not a valid IBAN.</p>
 */
public final class IbanValidator {

    // Per-country IBAN structure patterns. Each begins with the two-letter country code, so the
    // country code is the map key (the first two characters of the pattern). The patterns are
    // fully fixed-length, so a successful match also enforces the country's IBAN length.
    private static final String[] COUNTRY_FORMATS = {
            "AD\\d{10}[A-Z0-9]{12}",
            "AE\\d{21}",
            "AL\\d{10}[A-Z0-9]{16}",
            "AT\\d{18}",
            "AX\\d{16}",
            "AZ\\d{2}[A-Z]{4}[A-Z0-9]{20}",
            "BA\\d{18}",
            "BE\\d{14}",
            "BG\\d{2}[A-Z]{4}\\d{6}[A-Z0-9]{8}",
            "BH\\d{2}[A-Z]{4}[A-Z0-9]{14}",
            "BI\\d{25}",
            "BL\\d{12}[A-Z0-9]{11}\\d{2}",
            "BR\\d{25}[A-Z]{1}[A-Z0-9]{1}",
            "BY\\d{2}[A-Z0-9]{4}\\d{4}[A-Z0-9]{16}",
            "CH\\d{7}[A-Z0-9]{12}",
            "CR\\d{20}",
            "CY\\d{10}[A-Z0-9]{16}",
            "CZ\\d{22}",
            "DE\\d{20}",
            "DJ\\d{25}",
            "DK\\d{16}",
            "DO\\d{2}[A-Z0-9]{4}\\d{20}",
            "EE\\d{18}",
            "EG\\d{27}",
            "ES\\d{22}",
            "FI\\d{16}",
            "FK\\d{2}[A-Z]{2}\\d{12}",
            "FO\\d{16}",
            "FR\\d{12}[A-Z0-9]{11}\\d{2}",
            "GB\\d{2}[A-Z]{4}\\d{14}",
            "GE\\d{2}[A-Z]{2}\\d{16}",
            "GF\\d{12}[A-Z0-9]{11}\\d{2}",
            "GG\\d{2}[A-Z]{4}\\d{14}",
            "GI\\d{2}[A-Z]{4}[A-Z0-9]{15}",
            "GL\\d{16}",
            "GP\\d{12}[A-Z0-9]{11}\\d{2}",
            "GR\\d{9}[A-Z0-9]{16}",
            "GT\\d{2}[A-Z0-9]{24}",
            "HN\\d{2}[A-Z]{4}\\d{20}",
            "HR\\d{19}",
            "HU\\d{26}",
            "IE\\d{2}[A-Z]{4}\\d{14}",
            "IL\\d{21}",
            "IM\\d{2}[A-Z]{4}\\d{14}",
            "IQ\\d{2}[A-Z]{4}\\d{15}",
            "IS\\d{24}",
            "IT\\d{2}[A-Z]{1}\\d{10}[A-Z0-9]{12}",
            "JE\\d{2}[A-Z]{4}\\d{14}",
            "JO\\d{2}[A-Z]{4}\\d{4}[A-Z0-9]{18}",
            "KW\\d{2}[A-Z]{4}[A-Z0-9]{22}",
            "KZ\\d{5}[A-Z0-9]{13}",
            "LB\\d{6}[A-Z0-9]{20}",
            "LC\\d{2}[A-Z]{4}[A-Z0-9]{24}",
            "LI\\d{7}[A-Z0-9]{12}",
            "LT\\d{18}",
            "LU\\d{5}[A-Z0-9]{13}",
            "LV\\d{2}[A-Z]{4}[A-Z0-9]{13}",
            "LY\\d{23}",
            "MC\\d{12}[A-Z0-9]{11}\\d{2}",
            "MD\\d{2}[A-Z0-9]{20}",
            "ME\\d{20}",
            "MF\\d{12}[A-Z0-9]{11}\\d{2}",
            "MK\\d{5}[A-Z0-9]{10}\\d{2}",
            "MN\\d{18}",
            "MQ\\d{12}[A-Z0-9]{11}\\d{2}",
            "MR\\d{25}",
            "MT\\d{2}[A-Z]{4}\\d{5}[A-Z0-9]{18}",
            "MU\\d{2}[A-Z]{4}\\d{19}[A-Z]{3}",
            "NC\\d{12}[A-Z0-9]{11}\\d{2}",
            "NI\\d{2}[A-Z]{4}\\d{20}",
            "NL\\d{2}[A-Z]{4}\\d{10}",
            "NO\\d{13}",
            "OM\\d{5}[A-Z0-9]{16}",
            "PF\\d{12}[A-Z0-9]{11}\\d{2}",
            "PK\\d{2}[A-Z]{4}[A-Z0-9]{16}",
            "PL\\d{26}",
            "PM\\d{12}[A-Z0-9]{11}\\d{2}",
            "PS\\d{2}[A-Z]{4}[A-Z0-9]{21}",
            "PT\\d{23}",
            "QA\\d{2}[A-Z]{4}[A-Z0-9]{21}",
            "RE\\d{12}[A-Z0-9]{11}\\d{2}",
            "RO\\d{2}[A-Z]{4}[A-Z0-9]{16}",
            "RS\\d{20}",
            "RU\\d{16}[A-Z0-9]{15}",
            "SA\\d{4}[A-Z0-9]{18}",
            "SC\\d{2}[A-Z]{4}\\d{20}[A-Z]{3}",
            "SD\\d{16}",
            "SE\\d{22}",
            "SI\\d{17}",
            "SK\\d{22}",
            "SM\\d{2}[A-Z]{1}\\d{10}[A-Z0-9]{12}",
            "SO\\d{21}",
            "ST\\d{23}",
            "SV\\d{2}[A-Z]{4}\\d{20}",
            "TF\\d{12}[A-Z0-9]{11}\\d{2}",
            "TL\\d{21}",
            "TN\\d{22}",
            "TR\\d{8}[A-Z0-9]{16}",
            "UA\\d{8}[A-Z0-9]{19}",
            "VA\\d{20}",
            "VG\\d{2}[A-Z]{4}\\d{16}",
            "WF\\d{12}[A-Z0-9]{11}\\d{2}",
            "XK\\d{18}",
            "YE\\d{2}[A-Z]{4}\\d{4}[A-Z0-9]{18}",
            "YT\\d{12}[A-Z0-9]{11}\\d{2}",
    };

    private static final Map<String, Pattern> FORMATS_BY_COUNTRY = new HashMap<>(COUNTRY_FORMATS.length * 2);

    static {
        for (final String format : COUNTRY_FORMATS) {
            FORMATS_BY_COUNTRY.put(format.substring(0, 2), Pattern.compile(format));
        }
    }

    private IbanValidator() {
        // Utility class; not instantiable.
    }

    /**
     * Returns {@code true} when the value is a structurally valid IBAN with a correct checksum.
     *
     * @param iban the candidate IBAN with no spaces, may be {@code null}
     * @return {@code true} if the value is a valid IBAN for a known country
     */
    public static boolean isValid(final String iban) {

        if (iban == null || iban.length() < 2) {
            return false;
        }

        final Pattern format = FORMATS_BY_COUNTRY.get(iban.substring(0, 2));

        if (format == null || !format.matcher(iban).matches()) {
            return false;
        }

        return checksumIsValid(iban);

    }

    /**
     * Verifies the ISO 7064 MOD 97-10 checksum: move the first four characters to the end,
     * map letters to numbers (A=10 ... Z=35), and confirm the resulting integer is congruent
     * to 1 modulo 97. The remainder is accumulated digit by digit so no big-integer math is
     * needed.
     */
    private static boolean checksumIsValid(final String iban) {

        final String rearranged = iban.substring(4) + iban.substring(0, 4);

        int remainder = 0;

        for (int i = 0; i < rearranged.length(); i++) {

            final char c = rearranged.charAt(i);
            final int value;

            if (c >= '0' && c <= '9') {
                value = c - '0';
            } else if (c >= 'A' && c <= 'Z') {
                value = c - 'A' + 10;
            } else {
                // Should not happen once the structure pattern has matched, but guard anyway.
                return false;
            }

            // A letter contributes two decimal digits; everything else contributes one.
            remainder = (value > 9 ? remainder * 100 + value : remainder * 10 + value) % 97;

        }

        return remainder == 1;

    }

}
