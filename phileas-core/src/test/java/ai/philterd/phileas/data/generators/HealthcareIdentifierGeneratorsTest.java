/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.data.generators;

import ai.philterd.phileas.services.filters.regex.DeaNumberFilter;
import ai.philterd.phileas.services.filters.regex.MbiFilter;
import ai.philterd.phileas.services.filters.regex.NpiFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

public class HealthcareIdentifierGeneratorsTest {

    private final SecureRandom random = new SecureRandom();

    @Test
    public void generatedValuesPassTheirFiltersChecks() {

        for (int i = 0; i < 1000; i++) {
            final String npi = new NPIGenerator(random).random();
            Assertions.assertTrue(NpiFilter.isValid(npi), npi);
            final String dea = new DEANumberGenerator(random).random();
            Assertions.assertTrue(DeaNumberFilter.isValid(dea), dea);
            final String mbi = new MBIGenerator(random).random();
            Assertions.assertTrue(MbiFilter.isValid(mbi), mbi);
        }

    }

    @Test
    public void poolSizes() {

        Assertions.assertEquals(200_000_000L, new NPIGenerator(random).poolSize());
        Assertions.assertEquals(676_000_000L, new DEANumberGenerator(random).poolSize());
        Assertions.assertTrue(new MBIGenerator(random).poolSize() > 0);

    }

}
