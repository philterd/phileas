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

public class AgeAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(AgeAnonymizationServiceTest.class);

    /**
     * The realistic age anonymization replaces each digit with another digit and leaves every other
     * character untouched, so the result must be the same length, differ from the input, and match
     * the input character-for-character except that digit positions hold (possibly different) digits.
     */
    private static void assertOnlyDigitsReplaced(final String token, final String replacement) {

        Assertions.assertNotEquals(token, replacement);
        Assertions.assertEquals(token.length(), replacement.length());

        for (int i = 0; i < token.length(); i++) {
            if (Character.isDigit(token.charAt(i))) {
                Assertions.assertTrue(Character.isDigit(replacement.charAt(i)),
                        "digit positions must remain digits");
            } else {
                Assertions.assertEquals(token.charAt(i), replacement.charAt(i),
                        "non-digit characters must be preserved");
            }
        }

    }

    @Test
    public void constructor() {

        AnonymizationService anonymizationService = new AgeAnonymizationService(new SecureRandom(), AnonymizationMethod.REALISTIC);

        final String token = "18 years old";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Age: {}", replacement);
        assertOnlyDigitsReplaced(token, replacement);

    }

    @Test
    public void anonymize1() {

        AnonymizationService anonymizationService = new AgeAnonymizationService();

        final String token = "3.5yrs";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Age: {}", replacement);
        assertOnlyDigitsReplaced(token, replacement);

    }

    @Test
    public void anonymize2() {

        AnonymizationService anonymizationService = new AgeAnonymizationService();

        final String token = "18 years old";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Age: {}", replacement);
        assertOnlyDigitsReplaced(token, replacement);

    }

    @Test
    public void producesNonEmptyReplacement() {

        // SecureRandom is not deterministically seedable, so the replacement varies run to run;
        // verify the anonymizer still produces a non-empty replacement distinct from the input.
        final String token = "18 years old";

        final String first = new AgeAnonymizationService().anonymize(token);

        assertOnlyDigitsReplaced(token, first);

    }

    @Test
    public void anonymizeUUID() {

        AnonymizationService anonymizationService = new AgeAnonymizationService(new SecureRandom(), AnonymizationMethod.UUID);

        final String token = "18 years old";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Age: {}", replacement);
        Assertions.assertNotEquals(token, replacement);
        Assertions.assertTrue(replacement.length() >= 32);

    }

}
