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

import org.apache.commons.collections4.CollectionUtils;

import ai.philterd.phileas.services.context.ContextService;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class StateAbbreviationAnonymizationService extends AbstractAnonymizationService {

    public StateAbbreviationAnonymizationService(final ContextService contextService, final Random random, final AnonymizationMethod anonymizationMethod) {
        super(contextService, random, anonymizationMethod);
    }

    public StateAbbreviationAnonymizationService(final ContextService contextService, final Random random, final List<String> candidates) {
        super(contextService, random, candidates);
    }

    public StateAbbreviationAnonymizationService(final ContextService contextService, final Random random) {
        super(contextService, random);
    }

    public StateAbbreviationAnonymizationService(final ContextService contextService) {
        super(contextService);
    }

    private static final List<String> STATES = new LinkedList<>();

    static {

        // TODO: Don't duplicate this from StateFilter.
        STATES.add("AL");
        STATES.add("AK");
        STATES.add("AZ");
        STATES.add("AR");
        STATES.add("CA");
        STATES.add("CO");
        STATES.add("CT");
        STATES.add("DE");
        STATES.add("FL");
        STATES.add("GA");
        STATES.add("HI");
        STATES.add("ID");
        STATES.add("IL");
        STATES.add("IN");
        STATES.add("IA");
        STATES.add("KS");
        STATES.add("KY");
        STATES.add("LA");
        STATES.add("ME");
        STATES.add("MD");
        STATES.add("MA");
        STATES.add("MI");
        STATES.add("MN");
        STATES.add("MS");
        STATES.add("MO");
        STATES.add("MT");
        STATES.add("NE");
        STATES.add("NV");
        STATES.add("NH");
        STATES.add("NJ");
        STATES.add("NM");
        STATES.add("NY");
        STATES.add("NC");
        STATES.add("ND");
        STATES.add("OH");
        STATES.add("OK");
        STATES.add("OR");
        STATES.add("PA");
        STATES.add("RI");
        STATES.add("SC");
        STATES.add("SD");
        STATES.add("TN");
        STATES.add("TX");
        STATES.add("UT");
        STATES.add("VT");
        STATES.add("VA");
        STATES.add("WA");
        STATES.add("WV");
        STATES.add("WI");
        STATES.add("WY");

    }

    @Override
    public ContextService getContextService() {
        return contextService;
    }

    @Override
    public String anonymize(final String token) {

        if (anonymizationMethod == AnonymizationMethod.CUSTOM_LIST) {

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
            final int randomInt = generateInteger(0, STATES.size() - 1);

            String anonymized = STATES.get(randomInt);

            while (anonymized.equalsIgnoreCase(token)) {
                final int nextRandomInt = generateInteger(0, STATES.size() - 1);
                anonymized = STATES.get(nextRandomInt);
            }

            return anonymized;

        }

    }

}
