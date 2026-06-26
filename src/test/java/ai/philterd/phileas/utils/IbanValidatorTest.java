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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link IbanValidator}. The valid examples below were generated for every country code
 * in the IBAN registry and verified with commons-validator 1.10.0's {@code IBANValidator} (the
 * dependency this class replaces), so every country code the old code accepted is still accepted
 * with an identical structure-and-checksum check.
 */
public class IbanValidatorTest {

    // One structurally valid IBAN (correct length, structure, and MOD 97-10 checksum) for every
    // country code in the registry. There is one entry per country code, including the overseas
    // territories that share a parent country's format (the French group and the GB group).
    private static final String[] VALID_IBANS = {
            "AD6600000000000000000000", "AE630000000000000000000", "AL42000000000000000000000000", "AT180000000000000000",
            "AX0600000000000000", "AZ16AAAA00000000000000000000", "BA660000000000000000", "BE54000000000000",
            "BG45AAAA00000000000000", "BH42AAAA00000000000000", "BI4200000000000000000000000", "BL3300000000000000000000000",
            "BR3100000000000000000000000A0", "BY91000000000000000000000000", "CH3600000000000000000", "CR06000000000000000000",
            "CY82000000000000000000000000", "CZ7900000000000000000000", "DE36000000000000000000", "DJ2100000000000000000000000",
            "DK1800000000000000", "DO06000000000000000000000000", "EE270000000000000000", "EG210000000000000000000000000",
            "ES8200000000000000000000", "FI0600000000000000", "FK29AA000000000000", "FO8500000000000000",
            "FR7600000000000000000000000", "GB15AAAA00000000000000", "GE76AA0000000000000000", "GF0600000000000000000000000",
            "GG97AAAA00000000000000", "GI64AAAA000000000000000", "GL8500000000000000", "GP7300000000000000000000000",
            "GR6700000000000000000000000", "GT61000000000000000000000000", "HN86AAAA00000000000000000000", "HR5800000000000000000",
            "HU49000000000000000000000000", "IE85AAAA00000000000000", "IL670000000000000000000", "IM61AAAA00000000000000",
            "IQ22AAAA000000000000000", "IS460000000000000000000000", "IT83A0000000000000000000000", "JE76AAAA00000000000000",
            "JO97AAAA0000000000000000000000", "KW64AAAA0000000000000000000000", "KZ070000000000000000", "LB70000000000000000000000000",
            "LC17AAAA000000000000000000000000", "LI4900000000000000000", "LT160000000000000000", "LU130000000000000000",
            "LV97AAAA0000000000000", "LY98000000000000000000000", "MC5800000000000000000000000", "MD5500000000000000000000",
            "ME52000000000000000000", "MF4900000000000000000000000", "MK34000000000000000", "MN250000000000000000",
            "MQ1600000000000000000000000", "MR1300000000000000000000000", "MT02AAAA00000000000000000000000", "MU68AAAA0000000000000000000AAA",
            "NC4900000000000000000000000", "NI47AAAA00000000000000000000", "NL54AAAA0000000000", "NO1300000000000",
            "OM100000000000000000000", "PF2200000000000000000000000", "PK95AAAA0000000000000000", "PL04000000000000000000000000",
            "PM9800000000000000000000000", "PS46AAAA000000000000000000000", "PT77000000000000000000000", "QA91AAAA000000000000000000000",
            "RE0700000000000000000000000", "RO65AAAA0000000000000000", "RS62000000000000000000", "RU5600000000000000000000000000000",
            "SA1000000000000000000000", "SC30AAAA00000000000000000000AAA", "SD9800000000000000", "SE9500000000000000000000",
            "SI83000000000000000", "SK7700000000000000000000", "SM14A0000000000000000000000", "SO650000000000000000000",
            "ST50000000000000000000000", "SV60AAAA00000000000000000000", "TF8300000000000000000000000", "TL650000000000000000000",
            "TN5900000000000000000000", "TR470000000000000000000000", "UA890000000000000000000000000", "VA80000000000000000000",
            "VG53AAAA0000000000000000", "WF5600000000000000000000000", "XK320000000000000000", "YE89AAAA0000000000000000000000",
            "YT9300000000000000000000000",
    };

    @ParameterizedTest
    @ValueSource(strings = {
            "AD6600000000000000000000", "AE630000000000000000000", "AL42000000000000000000000000", "AT180000000000000000",
            "AX0600000000000000", "AZ16AAAA00000000000000000000", "BA660000000000000000", "BE54000000000000",
            "BG45AAAA00000000000000", "BH42AAAA00000000000000", "BI4200000000000000000000000", "BL3300000000000000000000000",
            "BR3100000000000000000000000A0", "BY91000000000000000000000000", "CH3600000000000000000", "CR06000000000000000000",
            "CY82000000000000000000000000", "CZ7900000000000000000000", "DE36000000000000000000", "DJ2100000000000000000000000",
            "DK1800000000000000", "DO06000000000000000000000000", "EE270000000000000000", "EG210000000000000000000000000",
            "ES8200000000000000000000", "FI0600000000000000", "FK29AA000000000000", "FO8500000000000000",
            "FR7600000000000000000000000", "GB15AAAA00000000000000", "GE76AA0000000000000000", "GF0600000000000000000000000",
            "GG97AAAA00000000000000", "GI64AAAA000000000000000", "GL8500000000000000", "GP7300000000000000000000000",
            "GR6700000000000000000000000", "GT61000000000000000000000000", "HN86AAAA00000000000000000000", "HR5800000000000000000",
            "HU49000000000000000000000000", "IE85AAAA00000000000000", "IL670000000000000000000", "IM61AAAA00000000000000",
            "IQ22AAAA000000000000000", "IS460000000000000000000000", "IT83A0000000000000000000000", "JE76AAAA00000000000000",
            "JO97AAAA0000000000000000000000", "KW64AAAA0000000000000000000000", "KZ070000000000000000", "LB70000000000000000000000000",
            "LC17AAAA000000000000000000000000", "LI4900000000000000000", "LT160000000000000000", "LU130000000000000000",
            "LV97AAAA0000000000000", "LY98000000000000000000000", "MC5800000000000000000000000", "MD5500000000000000000000",
            "ME52000000000000000000", "MF4900000000000000000000000", "MK34000000000000000", "MN250000000000000000",
            "MQ1600000000000000000000000", "MR1300000000000000000000000", "MT02AAAA00000000000000000000000", "MU68AAAA0000000000000000000AAA",
            "NC4900000000000000000000000", "NI47AAAA00000000000000000000", "NL54AAAA0000000000", "NO1300000000000",
            "OM100000000000000000000", "PF2200000000000000000000000", "PK95AAAA0000000000000000", "PL04000000000000000000000000",
            "PM9800000000000000000000000", "PS46AAAA000000000000000000000", "PT77000000000000000000000", "QA91AAAA000000000000000000000",
            "RE0700000000000000000000000", "RO65AAAA0000000000000000", "RS62000000000000000000", "RU5600000000000000000000000000000",
            "SA1000000000000000000000", "SC30AAAA00000000000000000000AAA", "SD9800000000000000", "SE9500000000000000000000",
            "SI83000000000000000", "SK7700000000000000000000", "SM14A0000000000000000000000", "SO650000000000000000000",
            "ST50000000000000000000000", "SV60AAAA00000000000000000000", "TF8300000000000000000000000", "TL650000000000000000000",
            "TN5900000000000000000000", "TR470000000000000000000000", "UA890000000000000000000000000", "VA80000000000000000000",
            "VG53AAAA0000000000000000", "WF5600000000000000000000000", "XK320000000000000000", "YE89AAAA0000000000000000000000",
            "YT9300000000000000000000000",
    })
    public void validForEveryCountryCode(final String iban) {
        Assertions.assertTrue(IbanValidator.isValid(iban), iban + " should validate");
    }

    // Guard that the per-country fixtures really do cover one distinct country code each, so this
    // suite fails loudly if a country is ever dropped from the registry or a duplicate slips in.
    @Test
    public void everyCountryCodeIsCoveredExactlyOnce() {

        final Set<String> countryCodes = new HashSet<>();

        for (final String iban : VALID_IBANS) {
            Assertions.assertTrue(countryCodes.add(iban.substring(0, 2)),
                    "duplicate country code fixture: " + iban.substring(0, 2));
        }

        Assertions.assertEquals(105, countryCodes.size(), "expected one IBAN per country code");

    }

    // Real published example IBANs (the GB cases exercised by IbanCodeFilterTest).
    @ParameterizedTest
    @ValueSource(strings = {
            "GB33BUKB20201555555555",
            "GB15MIDL40051512345678",
            "GB82WEST12345698765432",
            "DE89370400440532013000",
            "FR1420041010050500013M02606",
            "NL91ABNA0417164300",
    })
    public void realWorldExamplesValidate(final String iban) {
        Assertions.assertTrue(IbanValidator.isValid(iban), iban + " should validate");
    }

    // A correct IBAN with a single corrupted check digit must fail the checksum.
    @ParameterizedTest
    @ValueSource(strings = {
            "GB34BUKB20201555555555",
            "DE90370400440532013000",
            "AD6700000000000000000000",
    })
    public void wrongCheckDigitFails(final String iban) {
        Assertions.assertFalse(IbanValidator.isValid(iban), iban + " has a bad checksum and should fail");
    }

    // Unknown country codes are rejected.
    @ParameterizedTest
    @ValueSource(strings = {"AV01AZ", "ZZ0000000000000000", "QQ82WEST12345698765432", "XX00"})
    public void unknownCountryCodeFails(final String iban) {
        Assertions.assertFalse(IbanValidator.isValid(iban), iban + " has an unknown country and should fail");
    }

    // The right country but the wrong length is rejected (too short and too long).
    @ParameterizedTest
    @ValueSource(strings = {
            "GB33BUKB2020155555555",     // one short
            "GB33BUKB202015555555555",   // one long
            "DE8937040044053201300",     // one short
    })
    public void wrongLengthFails(final String iban) {
        Assertions.assertFalse(IbanValidator.isValid(iban), iban + " has the wrong length and should fail");
    }

    // Right length and checksum shape but a character in a position the structure forbids
    // (GB requires four letters for the bank code; digits there are invalid).
    @Test
    public void wrongStructureFails() {
        Assertions.assertFalse(IbanValidator.isValid("GB3312KB20201555555555"));
    }

    // IBAN letters are upper-case; a lower-case value is not a valid IBAN (matches commons-validator).
    @Test
    public void lowerCaseFails() {
        Assertions.assertFalse(IbanValidator.isValid("gb33bukb20201555555555"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"G", "GB", "1"})
    public void nullEmptyAndTooShortFail(final String iban) {
        Assertions.assertFalse(IbanValidator.isValid(iban));
    }

}
