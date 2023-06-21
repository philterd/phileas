/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.services.anonymization.LocationsAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class LocationsAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(IpAddressAnonymizationServiceTest.class);

    @Test
    public void anonymizeLocation1() {

        LocationsAnonymizationService anonymizationService = new LocationsAnonymizationService(new LocalAnonymizationCacheService());

        final String token = "Morgantown";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Location replacement: " + replacement);

    }

}
