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
package ai.philterd.test.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.model.services.DefaultContextService;
import ai.philterd.phileas.services.anonymization.MacAddressAnonymizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class MacAddressAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(MacAddressAnonymizationServiceTest.class);

    @Test
    public void anonymize1() {

        final AnonymizationService anonymizationService = new MacAddressAnonymizationService(new DefaultContextService());

        final String token = "00-14-22-04-25-37";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("MAC address replacement: {}", replacement);

    }

}
