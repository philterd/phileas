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

/**
 * Tests for {@link LuhnCheck}. The expected results were generated from, and verified against,
 * commons-validator 1.10.0's {@code LuhnCheckDigit.isValid} so this in-tree replacement behaves
 * identically to the dependency it replaced.
 */
public class LuhnCheckTest {

    // Numbers that pass the Luhn checksum. Includes the credit-card numbers exercised by
    // CreditCardFilterTest, vendor test PANs (Visa/Mastercard/Amex/Diners/Discover/JCB), the
    // classic "79927398713" Luhn example, and short two-digit values.
    @ParameterizedTest
    @ValueSource(strings = {
            "4532613702852251",
            "4556662764258031",
            "4929081870602661",
            "376454057275914",
            "346009657106278",
            "5567408136464012",
            "5100170632668801",
            "6011485579364263",
            "6011792597726344",
            "4111111111111111",
            "5500005555555559",
            "340000000000009",
            "30000000000004",
            "6011000000000004",
            "3530111333300000",
            "79927398713",
            "18",
            "26",
    })
    public void valid(final String code) {
        Assertions.assertTrue(LuhnCheck.isValid(code), code + " should be Luhn-valid");
    }

    // Numbers that fail the Luhn checksum: a single transposed/incremented check digit, obvious
    // sequences, and the neighbours of the classic example.
    @ParameterizedTest
    @ValueSource(strings = {
            "4532613702852252",
            "4556662764258030",
            "1234567812345678",
            "1234567890123456",
            "9876543219876543",
            "49927398717",
            "79927398710",
            "79927398712",
            "100",
            "1",
            "5",
            "11",
            "12",
            "99",
    })
    public void invalid(final String code) {
        Assertions.assertFalse(LuhnCheck.isValid(code), code + " should be Luhn-invalid");
    }

    // All-zero codes are rejected to match commons-validator, which treats a zero weighted sum as
    // an invalid code rather than a passing checksum.
    @ParameterizedTest
    @ValueSource(strings = {"0", "00", "000", "0000000000000000"})
    public void allZerosAreInvalid(final String code) {
        Assertions.assertFalse(LuhnCheck.isValid(code), code + " (all zeros) should be invalid");
    }

    // Anything containing a non-digit character is invalid.
    @ParameterizedTest
    @ValueSource(strings = {
            "abc",
            "4532-6137-0285-2251",
            "4532 6137 0285 2251",
            "453261370285225X",
            " 4532613702852251",
            "4532613702852251 ",
            "+18",
            "1.8",
    })
    public void nonDigitCharactersAreInvalid(final String code) {
        Assertions.assertFalse(LuhnCheck.isValid(code), code + " contains non-digits and should be invalid");
    }

    // Null and empty are invalid.
    @ParameterizedTest
    @NullAndEmptySource
    public void nullAndEmptyAreInvalid(final String code) {
        Assertions.assertFalse(LuhnCheck.isValid(code));
    }

    // Exhaustively check the ten single-digit check values for the classic Luhn base number:
    // exactly one of 79927398710..79927398719 is valid (the "...3" check digit).
    @Test
    public void exactlyOneCheckDigitValidatesForKnownBase() {

        final String base = "7992739871";
        int validCount = 0;
        String validValue = null;

        for (int d = 0; d <= 9; d++) {
            final String candidate = base + d;
            if (LuhnCheck.isValid(candidate)) {
                validCount++;
                validValue = candidate;
            }
        }

        Assertions.assertEquals(1, validCount, "exactly one check digit should validate");
        Assertions.assertEquals("79927398713", validValue);

    }

    // Appending the correct Luhn check digit to a body always validates, for every body in a range.
    @Test
    public void appendingComputedCheckDigitAlwaysValidates() {

        for (int body = 0; body <= 2000; body++) {

            final String bodyStr = Integer.toString(body);

            // Compute the Luhn check digit for bodyStr placed to the left of the check position.
            int sum = 0;
            boolean doubling = true; // the check digit sits at the right; the body's last digit doubles
            for (int i = bodyStr.length() - 1; i >= 0; i--) {
                int digit = bodyStr.charAt(i) - '0';
                if (doubling) {
                    digit *= 2;
                    if (digit > 9) {
                        digit -= 9;
                    }
                }
                sum += digit;
                doubling = !doubling;
            }
            final int checkDigit = (10 - (sum % 10)) % 10;
            final String full = bodyStr + checkDigit;

            // A non-zero body with its correct check digit must validate; an all-zero result must not.
            if (full.chars().allMatch(c -> c == '0')) {
                Assertions.assertFalse(LuhnCheck.isValid(full), full + " is all zeros");
            } else {
                Assertions.assertTrue(LuhnCheck.isValid(full), full + " has a correct check digit");
                // Corrupting the check digit by +1 (mod 10) must break it.
                final String corrupted = bodyStr + ((checkDigit + 1) % 10);
                Assertions.assertFalse(LuhnCheck.isValid(corrupted), corrupted + " has a wrong check digit");
            }

        }

    }

}
