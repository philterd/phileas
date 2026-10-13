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


import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

public class IpAddressAnonymizationService extends AbstractAnonymizationService {

    public IpAddressAnonymizationService(final SecureRandom random, final AnonymizationMethod anonymizationMethod) {
        super(random, anonymizationMethod);
    }

    public IpAddressAnonymizationService(final SecureRandom random, final List<String> candidates) {
        super(random, candidates);
    }

    public IpAddressAnonymizationService(final SecureRandom random) {
        super(random);
    }

    public IpAddressAnonymizationService() {
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
            return anonymizeRealistic(token, () -> getAnonymizedIpAddress(token));

        }

    }

    private String getAnonymizedIpAddress(String token) {

        if(token.contains(":")) {

            // IPv6: eight groups of four hexadecimal digits.
            final StringBuilder sb = new StringBuilder();

            for(int group = 0; group < 8; group++) {
                if(group > 0) {
                    sb.append(':');
                }
                sb.append(String.format("%04x", random.nextInt(0x10000)));
            }

            return sb.toString();

        } else {

            // IPv4: four octets from 0 to 255.
            return generateInteger(0, 255) + "."
                    + generateInteger(0, 255) + "."
                    + generateInteger(0, 255) + "."
                    + generateInteger(0, 255);

        }

    }



}
