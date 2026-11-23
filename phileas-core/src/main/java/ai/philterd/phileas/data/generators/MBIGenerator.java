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
import ai.philterd.phileas.services.filters.regex.MbiFilter;

import java.security.SecureRandom;

/**
 * Generates random Medicare Beneficiary Identifiers (MBI) in the format CMS publishes, unformatted.
 */
public class MBIGenerator implements DataGenerator.Generator<String> {

    private static final String LETTERS = MbiFilter.LETTERS;
    private static final String LETTERS_AND_DIGITS = MbiFilter.LETTERS + "0123456789";
    private static final String DIGITS = "0123456789";

    // The characters allowed in each of the eleven positions.
    private static final String[] POSITIONS = {"123456789", LETTERS, LETTERS_AND_DIGITS, DIGITS, LETTERS,
            LETTERS_AND_DIGITS, DIGITS, LETTERS, LETTERS, DIGITS, DIGITS};

    private final SecureRandom random;

    /**
     * Creates a new MBI generator.
     * @param random The {@link SecureRandom} to use.
     */
    public MBIGenerator(final SecureRandom random) {
        this.random = random;
    }

    @Override
    public String random() {
        final StringBuilder sb = new StringBuilder(POSITIONS.length);
        for(final String allowed : POSITIONS) {
            sb.append(allowed.charAt(random.nextInt(allowed.length())));
        }
        return sb.toString();
    }

    @Override
    public long poolSize() {
        long size = 1;
        for(final String allowed : POSITIONS) {
            size *= allowed.length();
        }
        return size;
    }

}
