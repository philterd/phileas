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
package ai.philterd.phileas.services.anonymization;

import ai.philterd.phileas.data.generators.CanadaSINGenerator;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Replaces a Canadian SIN with a random Luhn-valid SIN (beginning with a digit issued to
 * individuals), keeping the separators of the original.
 */
public class CanadaSinAnonymizationService extends AbstractAnonymizationService {

    public CanadaSinAnonymizationService(final SecureRandom random, final AnonymizationMethod anonymizationMethod) {
        super(random, anonymizationMethod);
    }

    public CanadaSinAnonymizationService(final SecureRandom random, final List<String> candidates) {
        super(random, candidates);
    }

    public CanadaSinAnonymizationService(final SecureRandom random) {
        super(random);
    }

    public CanadaSinAnonymizationService() {
        super();
    }

    @Override
    public String anonymize(final String token) {

        if (anonymizationMethod == AnonymizationMethod.FROM_LIST) {

            return anonymizeFromList(token);

        } else if (anonymizationMethod == AnonymizationMethod.UUID) {

            return UUID.randomUUID().toString();

        } else {

            // REALISTIC_REPLACE
            return anonymizeRealistic(token, () -> getAnonymizedSin(token));

        }

    }

    /**
     * A random SIN laid out like the token: each of the token's digits is replaced in order, and
     * everything else (hyphens, spaces, a line break) is kept.
     */
    private String getAnonymizedSin(final String token) {

        final String digits = new CanadaSINGenerator(random).random().replace("-", "");

        if (token.replaceAll("\\D", "").length() != digits.length()) {
            return new CanadaSINGenerator(random).random();
        }

        final StringBuilder sb = new StringBuilder();
        int next = 0;

        for (int i = 0; i < token.length(); i++) {
            final char c = token.charAt(i);
            sb.append(Character.isDigit(c) ? digits.charAt(next++) : c);
        }

        return sb.toString();

    }

}
