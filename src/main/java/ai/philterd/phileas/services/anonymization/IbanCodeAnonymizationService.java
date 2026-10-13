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

import ai.philterd.phileas.data.DataGenerator;
import ai.philterd.phileas.data.DefaultDataGenerator;
import ai.philterd.phileas.utils.IbanValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class IbanCodeAnonymizationService extends AbstractAnonymizationService {

    private static final Logger LOGGER = LogManager.getLogger(IbanCodeAnonymizationService.class);

    private DataGenerator dataGenerator;

    public IbanCodeAnonymizationService(final SecureRandom random, final AnonymizationMethod anonymizationMethod) {
        super(random, anonymizationMethod);

        try {
            this.dataGenerator = new DefaultDataGenerator(random);
        } catch (IOException e) {
            LOGGER.error("Could not initialize data generator.", e);
        }
    }

    public IbanCodeAnonymizationService(final SecureRandom random, final List<String> candidates) {
        super(random, candidates);

        try {
            this.dataGenerator = new DefaultDataGenerator(random);
        } catch (IOException e) {
            LOGGER.error("Could not initialize data generator.", e);
        }
    }

    public IbanCodeAnonymizationService(final SecureRandom random) {
        super(random);

        try {
            this.dataGenerator = new DefaultDataGenerator(random);
        } catch (IOException e) {
            LOGGER.error("Could not initialize data generator.", e);
        }
    }

    public IbanCodeAnonymizationService() {
        super();

        try {
            this.dataGenerator = new DefaultDataGenerator(random);
        } catch (IOException e) {
            LOGGER.error("Could not initialize data generator.", e);
        }
    }

    @Override
    public String anonymize(final String token) {

        if (anonymizationMethod == AnonymizationMethod.FROM_LIST) {

            return anonymizeFromList(token);

        } else if (anonymizationMethod == AnonymizationMethod.UUID) {

            return java.util.UUID.randomUUID().toString();

        } else {

            // REALISTIC_REPLACE
            return anonymizeRealistic(token, () -> getAnonymizedIban(token));

        }

    }

    /**
     * A random IBAN laid out like the token: the country code and any spaces are kept, each character
     * of the account number (BBAN) is replaced with a random one of the same kind, and the check
     * digits are computed, so a replacement for a valid IBAN is also valid. A token that does not
     * begin with a country code and two digits is replaced with a generated IBAN.
     */
    private String getAnonymizedIban(final String token) {

        if (!token.matches("(?s)[A-Za-z]{2}[0-9]{2}.*[A-Za-z0-9].*")) {
            return dataGenerator.iban().random();
        }

        final String bban = replaceLikeForLike(token.substring(4));
        final String compactBban = bban.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        final String checkDigits = IbanValidator.checkDigits(token.substring(0, 2).toUpperCase(Locale.ROOT), compactBban);

        return token.substring(0, 2) + checkDigits + bban;

    }

}
