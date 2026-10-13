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

import ai.philterd.phileas.data.generators.VINGenerator;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

public class VinAnonymizationService extends AbstractAnonymizationService {

    public VinAnonymizationService(final SecureRandom random, final AnonymizationMethod anonymizationMethod) {
        super(random, anonymizationMethod);
    }

    public VinAnonymizationService(final SecureRandom random, final List<String> candidates) {
        super(random, candidates);
    }

    public VinAnonymizationService(final SecureRandom random) {
        super(random);
    }

    public VinAnonymizationService() {
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
            // A VIN uses digits and the letters A-Z excluding I, O, and Q (which are disallowed to
            // avoid confusion with 1 and 0), with a check digit in position 9.
            final VINGenerator vinGenerator = new VINGenerator(random, true);

            return anonymizeRealistic(token, () -> vinGenerator.random());

        }

    }

}
