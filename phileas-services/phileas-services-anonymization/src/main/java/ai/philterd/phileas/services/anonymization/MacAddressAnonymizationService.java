/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.services.AnonymizationCacheService;

import java.security.SecureRandom;

public class MacAddressAnonymizationService extends AbstractAnonymizationService {

    private SecureRandom secureRandom;

    public MacAddressAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);

        this.secureRandom = new SecureRandom();

    }

    @Override
    public String anonymize(String token) {

        byte[] macAddr = new byte[6];
        secureRandom.nextBytes(macAddr);

        final StringBuilder sb = new StringBuilder(18);

        for(byte b : macAddr){

            if(sb.length() > 0) {
                sb.append(":");
            }

            sb.append(String.format("%02x", b));

        }

        return sb.toString();

    }

}
