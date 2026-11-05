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

import java.security.SecureRandom;

/**
 * Generates random U.S. Individual Taxpayer Identification Numbers (ITIN) in the form
 * {@code 9XX-XX-XXXX}, with fourth and fifth digits in a range the IRS issues.
 */
public class ITINGenerator implements DataGenerator.Generator<String> {

    /** The fourth and fifth digits the IRS issues: 50-65, 70-88, 90-92, and 94-99. */
    private static final int[] GROUPS = groups();

    private final SecureRandom random;

    /**
     * Creates a new ITIN generator.
     * @param random The {@link SecureRandom} to use.
     */
    public ITINGenerator(final SecureRandom random) {
        this.random = random;
    }

    @Override
    public String random() {
        final int area = random.nextInt(100); // 900-999
        final int group = GROUPS[random.nextInt(GROUPS.length)];
        final int serial = random.nextInt(10000); // 0000-9999
        return String.format("9%02d-%02d-%04d", area, group, serial);
    }

    @Override
    public long poolSize() {
        return 100L * GROUPS.length * 10000L;
    }

    private static int[] groups() {
        final int[][] ranges = {{50, 65}, {70, 88}, {90, 92}, {94, 99}};
        int count = 0;
        for (final int[] range : ranges) {
            count += range[1] - range[0] + 1;
        }
        final int[] groups = new int[count];
        int i = 0;
        for (final int[] range : ranges) {
            for (int group = range[0]; group <= range[1]; group++) {
                groups[i++] = group;
            }
        }
        return groups;
    }

}
