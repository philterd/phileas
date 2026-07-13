/*
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.data.generators;

import ai.philterd.phileas.data.DataGenerator;

import java.security.SecureRandom;

/**
 * Generates random U.S. Employer Identification Numbers (EIN) in the canonical form {@code NN-NNNNNNN},
 * using a two-digit prefix the IRS currently issues.
 */
public class EINGenerator implements DataGenerator.Generator<String> {

    /** The two-digit prefixes the IRS currently issues. */
    private static final int[] PREFIXES = {
            1, 2, 3, 4, 5, 6,
            10, 11, 12, 13, 14, 15, 16,
            20, 21, 22, 23, 24, 25, 26, 27,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47, 48,
            50, 51, 52, 53, 54, 55, 56, 57, 58, 59,
            60, 61, 62, 63, 64, 65, 66, 67, 68,
            71, 72, 73, 74, 75, 76, 77,
            80, 81, 82, 83, 84, 85, 86, 87, 88,
            90, 91, 92, 93, 94, 95, 98, 99};

    private final SecureRandom random;

    /**
     * Creates a new EIN generator.
     * @param random The {@link SecureRandom} to use.
     */
    public EINGenerator(final SecureRandom random) {
        this.random = random;
    }

    @Override
    public String random() {
        final int prefix = PREFIXES[random.nextInt(PREFIXES.length)];
        final int serial = random.nextInt(10000000); // 0000000-9999999
        return String.format("%02d-%07d", prefix, serial);
    }

    @Override
    public long poolSize() {
        return (long) PREFIXES.length * 10000000L;
    }

}
