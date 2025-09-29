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
package ai.philterd.test.phileas.model.anonymization;

import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.model.anonymization.ZipCodeAnonymizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class ZipCodeAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(ZipCodeAnonymizationServiceTest.class);

    @Test
    public void anonymize() {

        AnonymizationService anonymizationService = new ZipCodeAnonymizationService();

        final String token = "90210";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Zip code replacement: {}", replacement);

    }

}
