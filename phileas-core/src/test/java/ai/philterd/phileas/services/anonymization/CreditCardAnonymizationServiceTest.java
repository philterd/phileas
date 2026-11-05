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

import ai.philterd.phileas.services.context.DefaultContextService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public class CreditCardAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(CreditCardAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new CreditCardAnonymizationService();

        final String token = "abcd1234";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Credit Card: {}", replacement);
        Assertions.assertNotEquals(token, replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertTrue(Pattern.matches("^[0-9-]*$", replacement));

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new CreditCardAnonymizationService();

        final String token = "April 1, 2019";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Credit Card: {}", replacement);
        Assertions.assertNotEquals(token, replacement);
        Assertions.assertNotNull(replacement);
        Assertions.assertTrue(Pattern.matches("^[0-9-]*$", replacement));

    }

    @Test
    public void anonymizeUUID() {

        AnonymizationService anonymizationService = new CreditCardAnonymizationService(new SecureRandom(), AnonymizationMethod.UUID);

        final String token = "abcd1234";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Credit Card: {}", replacement);
        Assertions.assertNotEquals(token, replacement);
        Assertions.assertTrue(replacement.length() >= 32);

    }

    @Test
    public void brandPrefixLengthKeepsOnlyTheDigitsThatIdentifyTheBrand() {

        // Visa, MasterCard, American Express, Diners Club, Discover, and JCB, at the edges of their ranges.
        Assertions.assertEquals(1, CreditCardAnonymizationService.brandPrefixLength("4111111111111111"));
        Assertions.assertEquals(2, CreditCardAnonymizationService.brandPrefixLength("5105105105105100"));
        Assertions.assertEquals(2, CreditCardAnonymizationService.brandPrefixLength("5555555555554444"));
        Assertions.assertEquals(4, CreditCardAnonymizationService.brandPrefixLength("2221000000000009"));
        Assertions.assertEquals(4, CreditCardAnonymizationService.brandPrefixLength("2720990000000007"));
        Assertions.assertEquals(2, CreditCardAnonymizationService.brandPrefixLength("378282246310005"));
        Assertions.assertEquals(2, CreditCardAnonymizationService.brandPrefixLength("341111111111111"));
        Assertions.assertEquals(3, CreditCardAnonymizationService.brandPrefixLength("30569309025904"));
        Assertions.assertEquals(2, CreditCardAnonymizationService.brandPrefixLength("36259600000004"));
        Assertions.assertEquals(2, CreditCardAnonymizationService.brandPrefixLength("38520000023237"));
        Assertions.assertEquals(4, CreditCardAnonymizationService.brandPrefixLength("6011111111111117"));
        Assertions.assertEquals(2, CreditCardAnonymizationService.brandPrefixLength("6500000000000002"));
        Assertions.assertEquals(2, CreditCardAnonymizationService.brandPrefixLength("3530111333300000"));
        Assertions.assertEquals(4, CreditCardAnonymizationService.brandPrefixLength("213100000000000"));
        Assertions.assertEquals(4, CreditCardAnonymizationService.brandPrefixLength("180000000000000"));

        // Numbers of no known brand keep only their first digit.
        for (final String digits : new String[]{"2220000000000000", "2721000000000000", "3060000000000000", "3300000000000000",
                "5000000000000000", "5600000000000000", "6012000000000000", "6400000000000000", "1234567890123456", "9999999999999999"}) {
            Assertions.assertEquals(1, CreditCardAnonymizationService.brandPrefixLength(digits), digits);
        }

    }

}
