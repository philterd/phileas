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
import ai.philterd.phileas.services.filters.regex.DeaNumberFilter;

import java.security.SecureRandom;

/**
 * Generates random DEA registration numbers: two capital letters followed by seven digits, ending in the
 * check digit.
 */
public class DEANumberGenerator implements DataGenerator.Generator<String> {

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final SecureRandom random;

    /**
     * Creates a new DEA number generator.
     * @param random The {@link SecureRandom} to use.
     */
    public DEANumberGenerator(final SecureRandom random) {
        this.random = random;
    }

    @Override
    public String random() {
        final String prefix = "" + LETTERS.charAt(random.nextInt(26)) + LETTERS.charAt(random.nextInt(26));
        final String six = String.format("%06d", random.nextInt(1_000_000));
        return prefix + six + DeaNumberFilter.checkDigit(six);
    }

    @Override
    public long poolSize() {
        return 26L * 26L * 1_000_000L;
    }

}
