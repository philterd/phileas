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
package ai.philterd.phileas.services.anonymization;

import ai.philterd.phileas.data.generators.CreditCardNumberGenerator;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

public class CreditCardAnonymizationService extends AbstractAnonymizationService {

    public CreditCardAnonymizationService(final SecureRandom random, final AnonymizationMethod anonymizationMethod) {
        super(random, anonymizationMethod);
    }

    public CreditCardAnonymizationService(final SecureRandom random, final List<String> candidates) {
        super(random, candidates);
    }

    public CreditCardAnonymizationService(final SecureRandom random) {
        super(random);
    }

    public CreditCardAnonymizationService() {
        super();
    }

    @Override
    public String anonymize(final String token) {

        if (anonymizationMethod == AnonymizationMethod.FROM_LIST) {

            return anonymizeFromList(token);

        } else if (anonymizationMethod == AnonymizationMethod.UUID) {

            return java.util.UUID.randomUUID().toString();

        } else {

            // REALISTIC_REPLACE
            return anonymizeRealistic(token, () -> getAnonymizedCreditCardNumber(token));

        }

    }

    /**
     * A random card number laid out like the token: the same length and separators, the same card
     * brand (the leading digits that identify the brand are kept), random digits in between, and a
     * valid Luhn check digit. A token with too few digits to be a card number is replaced with a
     * generated one.
     */
    private String getAnonymizedCreditCardNumber(final String token) {

        final String digits = token.replaceAll("[^0-9]", "");

        if (digits.length() < 12) {
            return new CreditCardNumberGenerator(random, true).random();
        }

        final int[] replaced = new int[digits.length()];
        final int kept = brandPrefixLength(digits);

        for (int i = 0; i < replaced.length - 1; i++) {
            replaced[i] = i < kept ? digits.charAt(i) - '0' : random.nextInt(10);
        }

        replaced[replaced.length - 1] = luhnCheckDigit(replaced);

        final StringBuilder sb = new StringBuilder(token.length());
        int next = 0;

        for (int i = 0; i < token.length(); i++) {
            final char c = token.charAt(i);
            sb.append(c >= '0' && c <= '9' ? (char) ('0' + replaced[next++]) : c);
        }

        return sb.toString();

    }

    /**
     * How many leading digits identify the card brand, following the brand patterns the credit card
     * filter's {@code onlyValid} option checks: Visa (4), MasterCard (51 to 55, and 2221 to 2720),
     * American Express (34, 37), Diners Club (300 to 305, 36, 38), Discover (6011, 65), and JCB (35,
     * 2131, 1800). Only the first digit is kept for a number of no known brand.
     */
    static int brandPrefixLength(final String digits) {

        final int first4 = Integer.parseInt(digits.substring(0, 4));

        if ((first4 >= 2221 && first4 <= 2720) || first4 == 2131 || first4 == 1800 || first4 == 6011) {
            return 4;
        } else if (first4 >= 3000 && first4 <= 3059) {
            return 3;
        } else if (digits.matches("(?:3[4-8]|5[1-5]|65).*")) {
            return 2;
        } else {
            return 1;
        }

    }

    /** The Luhn check digit for every digit but the last. */
    private static int luhnCheckDigit(final int[] digits) {

        int sum = 0;
        boolean doubled = true;

        for (int i = digits.length - 2; i >= 0; i--) {
            int value = digits[i];
            if (doubled) {
                value *= 2;
                if (value > 9) {
                    value -= 9;
                }
            }
            sum += value;
            doubled = !doubled;
        }

        return (10 - (sum % 10)) % 10;

    }

}
