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

import ai.philterd.phileas.services.context.DefaultContextService;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HospitalAbbreviationAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(HospitalAbbreviationAnonymizationServiceTest.class);

    @Test
    public void anonymize() {

        AnonymizationService anonymizationService = new HospitalAbbreviationAnonymizationService(new DefaultContextService());

        final String token = "Plateau Medical Center";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("Hospital abbreviation: " + replacement);

        Assertions.assertTrue(StringUtils.isAllUpperCase(replacement));
        Assertions.assertEquals(-1, replacement.indexOf(" "));

    }

}
