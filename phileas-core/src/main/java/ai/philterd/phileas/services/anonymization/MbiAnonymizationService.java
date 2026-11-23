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

import ai.philterd.phileas.data.generators.MBIGenerator;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Replaces an MBI with a random MBI, in the card's grouping when the token uses it.
 */
public class MbiAnonymizationService extends AbstractAnonymizationService {

    public MbiAnonymizationService(final SecureRandom random, final AnonymizationMethod anonymizationMethod) {
        super(random, anonymizationMethod);
    }

    public MbiAnonymizationService(final SecureRandom random, final List<String> candidates) {
        super(random, candidates);
    }

    public MbiAnonymizationService(final SecureRandom random) {
        super(random);
    }

    public MbiAnonymizationService() {
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
            return anonymizeRealistic(token, () -> getAnonymizedMbi(token));

        }

    }

    /** A random MBI, hyphenated like the token when the token is hyphenated. */
    private String getAnonymizedMbi(final String token) {

        final String mbi = new MBIGenerator(random).random();

        if (token.indexOf('-') >= 0) {
            return mbi.substring(0, 4) + "-" + mbi.substring(4, 7) + "-" + mbi.substring(7);
        }

        return mbi;

    }

}
