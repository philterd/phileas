/*
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.data.generators;

import ai.philterd.phileas.data.DataGenerator;

import ai.philterd.phileas.utils.IbanValidator;

import java.security.SecureRandom;

/**
 * Generates random IBANs: German IBANs with a random account number and valid check digits.
 */
public class IBANGenerator implements DataGenerator.Generator<String> {
    private final SecureRandom random;

    /**
     * Creates a new IBAN generator.
     * @param random The {@link SecureRandom} to use.
     */
    public IBANGenerator(final SecureRandom random) {
        this.random = random;
    }

    @Override
    public String random() {
        // A German IBAN: DE, two check digits, and an 18-digit BBAN.
        final StringBuilder bban = new StringBuilder(18);
        for (int i = 0; i < 18; i++) {
            bban.append(random.nextInt(10));
        }
        return "DE" + IbanValidator.checkDigits("DE", bban.toString()) + bban;
    }

    @Override
    public long poolSize() {
        return 1_000_000_000_000_000_000L;
    }

}
