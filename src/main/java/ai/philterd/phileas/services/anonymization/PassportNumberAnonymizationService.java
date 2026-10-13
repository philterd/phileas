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

import ai.philterd.phileas.utils.CollectionUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

public class PassportNumberAnonymizationService extends AbstractAnonymizationService {

    /** The two-digit prefixes of the US passport numbers the passport filter detects. */
    private static final String[] US_PREFIXES = {
            "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17",
            "20", "21", "30", "40", "50", "60", "80", "90"};

    public PassportNumberAnonymizationService(final SecureRandom random, final AnonymizationMethod anonymizationMethod) {
        super(random, anonymizationMethod);
    }

    public PassportNumberAnonymizationService(final SecureRandom random, final List<String> candidates) {
        super(random, candidates);
    }

    public PassportNumberAnonymizationService(final SecureRandom random) {
        super(random);
    }

    public PassportNumberAnonymizationService() {
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

            return java.util.UUID.randomUUID().toString();

        } else {

            // REALISTIC_REPLACE
            return anonymizeRealistic(token, () -> getAnonymizedPassportNumber(token));

        }

    }

    /**
     * A random passport number with the token's shape. A token that begins with two digits and is not
     * a nine-digit number is given one of the two-digit prefixes the passport filter detects, so the
     * replacement is a value that filter would detect.
     */
    private String getAnonymizedPassportNumber(final String token) {

        final String replaced = replaceLikeForLike(token);

        if (token.length() > 2 && Character.isDigit(token.charAt(0)) && Character.isDigit(token.charAt(1))
                && !token.matches("[0-9]{9}")) {
            return US_PREFIXES[random.nextInt(US_PREFIXES.length)] + replaced.substring(2);
        }

        return replaced;

    }

}