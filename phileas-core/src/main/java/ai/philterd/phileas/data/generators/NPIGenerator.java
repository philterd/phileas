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

import ai.philterd.phileas.data.DataGenerator;
import ai.philterd.phileas.services.filters.regex.NpiFilter;

import java.security.SecureRandom;

/**
 * Generates random U.S. National Provider Identifiers (NPI): ten digits beginning with 1 or 2, ending in
 * the NPI check digit.
 */
public class NPIGenerator implements DataGenerator.Generator<String> {

    private final SecureRandom random;

    /**
     * Creates a new NPI generator.
     * @param random The {@link SecureRandom} to use.
     */
    public NPIGenerator(final SecureRandom random) {
        this.random = random;
    }

    @Override
    public String random() {
        final String firstNine = (1 + random.nextInt(2)) + String.format("%08d", random.nextInt(100_000_000));
        return firstNine + NpiFilter.checkDigit(firstNine);
    }

    @Override
    public long poolSize() {
        return 2L * 100_000_000L;
    }

}
