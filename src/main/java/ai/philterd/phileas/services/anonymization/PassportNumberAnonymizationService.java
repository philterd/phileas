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

import ai.philterd.phileas.services.context.ContextService;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PassportNumberAnonymizationService extends AbstractAnonymizationService {

    public PassportNumberAnonymizationService(final ContextService contextService, final Random random, final AnonymizationMethod anonymizationMethod) {
        super(contextService, random, anonymizationMethod);
    }

    public PassportNumberAnonymizationService(final ContextService contextService, final Random random, final List<String> candidates) {
        super(contextService, random, candidates);
    }

    public PassportNumberAnonymizationService(final ContextService contextService, final Random random) {
        super(contextService, random);
    }

    public PassportNumberAnonymizationService(final ContextService contextService) {
        super(contextService);
    }

    @Override
    public ContextService getContextService() {
        return contextService;
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
            String anonymized = getAnonymizedPassportNumber();

            while (anonymized.equalsIgnoreCase(token)) {
                anonymized = getAnonymizedPassportNumber();
            }

            return anonymized;

        }

    }

    private String getAnonymizedPassportNumber() {

        final byte[] macAddr = new byte[6];
        random.nextBytes(macAddr);

        final StringBuilder sb = new StringBuilder(18);

        for(byte b : macAddr){

            if(!sb.isEmpty()) {
                sb.append(":");
            }

            sb.append(String.format("%02x", b));

        }

        return sb.toString();

    }

}