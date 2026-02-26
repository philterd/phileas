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

import ai.philterd.phileas.data.DataGenerator;
import ai.philterd.phileas.data.DefaultDataGenerator;
import ai.philterd.phileas.services.context.ContextService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class SurnameAnonymizationService extends AbstractAnonymizationService {

    private static final Logger LOGGER = LogManager.getLogger(SurnameAnonymizationService.class);

    private DataGenerator dataGenerator;

    public SurnameAnonymizationService(final ContextService contextService, final Random random, final AnonymizationMethod anonymizationMethod) {
        super(contextService, random, anonymizationMethod);

        try {
            this.dataGenerator = new DefaultDataGenerator(random);
        } catch (IOException e) {
            LOGGER.error("Could not initialize data generator.", e);
        }
    }

    public SurnameAnonymizationService(final ContextService contextService, final Random random, final List<String> candidates) {
        super(contextService, random, candidates);

        try {
            this.dataGenerator = new DefaultDataGenerator(random);
        } catch (IOException e) {
            LOGGER.error("Could not initialize data generator.", e);
        }
    }

    public SurnameAnonymizationService(final ContextService contextService, final Random random) {
        super(contextService, random);

        try {
            this.dataGenerator = new DefaultDataGenerator(random);
        } catch (IOException e) {
            LOGGER.error("Could not initialize data generator.", e);
        }
    }

    public SurnameAnonymizationService(final ContextService contextService) {
        super(contextService);

        try {
            this.dataGenerator = new DefaultDataGenerator(random);
        } catch (IOException e) {
            LOGGER.error("Could not initialize data generator.", e);
        }
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
            String anonymized = dataGenerator.surnames().random();

            while (anonymized.equalsIgnoreCase(token)) {
                anonymized = dataGenerator.surnames().random();
            }

            return anonymized;

        }

    }

}
