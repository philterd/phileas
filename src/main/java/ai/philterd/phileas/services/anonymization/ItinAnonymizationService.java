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

import ai.philterd.phileas.data.generators.ITINGenerator;
import ai.philterd.phileas.utils.CollectionUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Replaces an ITIN with a random ITIN-shaped value (beginning with 9, with fourth and fifth digits
 * in a range the IRS issues), keeping the separators of the original.
 */
public class ItinAnonymizationService extends AbstractAnonymizationService {

    public ItinAnonymizationService(final SecureRandom random, final AnonymizationMethod anonymizationMethod) {
        super(random, anonymizationMethod);
    }

    public ItinAnonymizationService(final SecureRandom random, final List<String> candidates) {
        super(random, candidates);
    }

    public ItinAnonymizationService(final SecureRandom random) {
        super(random);
    }

    public ItinAnonymizationService() {
        super();
    }

    @Override
    public String anonymize(final String token) {

        if (anonymizationMethod == AnonymizationMethod.FROM_LIST) {

            if (CollectionUtils.isNotEmpty(candidates)) {

                String anonymized = candidates.get(random.nextInt(candidates.size()));
                while (anonymized.equalsIgnoreCase(token)) {
                    anonymized = candidates.get(random.nextInt(candidates.size()));
                }
                return anonymized;

            } else {

                // Provided list was empty - return a random UUID.
                return UUID.randomUUID().toString();

            }

        } else if (anonymizationMethod == AnonymizationMethod.UUID) {

            return UUID.randomUUID().toString();

        } else {

            // REALISTIC_REPLACE
            String anonymized = getAnonymizedItin(token);

            while (anonymized.equalsIgnoreCase(token)) {
                anonymized = getAnonymizedItin(token);
            }

            return anonymized;

        }

    }

    /**
     * A random ITIN laid out like the token: each of the token's digits is replaced in order, and
     * everything else (hyphens, spaces, a line break) is kept.
     */
    private String getAnonymizedItin(final String token) {

        final String digits = new ITINGenerator(random).random().replace("-", "");

        if (token.replaceAll("\\D", "").length() != digits.length()) {
            return new ITINGenerator(random).random();
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
