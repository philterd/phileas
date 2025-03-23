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

import ai.philterd.phileas.model.services.CacheService;
import org.apache.commons.lang3.RandomStringUtils;

public class BitcoinAddressAnonymizationService extends AbstractAnonymizationService {

    public BitcoinAddressAnonymizationService(CacheService anonymizationCacheService) {
        super(anonymizationCacheService);
    }

    @Override
    public String anonymize(String token) {

        // See PHL-117: Just generating a random alphanumeric string.
        return RandomStringUtils.randomAlphanumeric(32);

    }

}
