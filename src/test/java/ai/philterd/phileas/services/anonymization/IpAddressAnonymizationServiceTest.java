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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class IpAddressAnonymizationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(IpAddressAnonymizationServiceTest.class);

    @Test
    public void anonymizeIPv4() {

        AnonymizationService anonymizationService = new IpAddressAnonymizationService(new DefaultContextService());

        final String token = "192.168.1.1";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("IP replacement: " + replacement);

    }

    @Test
    public void anonymizeIPv6() {

        AnonymizationService anonymizationService = new IpAddressAnonymizationService(new DefaultContextService());

        final String token = "2001:0db8:85a3:08d3:1319:8a2e:0370:7344";
        final String replacement = anonymizationService.anonymize(token);

        LOGGER.info("IP replacement: " + replacement);

    }

}
