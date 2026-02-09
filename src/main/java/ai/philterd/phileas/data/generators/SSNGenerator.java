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

import java.util.Random;

/**
 * Generates random Social Security Numbers (SSN).
 */
public class SSNGenerator implements DataGenerator.Generator<String> {
    private final Random random;

    /**
     * Creates a new SSN generator.
     * @param random The {@link Random} to use.
     */
    public SSNGenerator(final Random random) {
        this.random = random;
    }

    @Override
    public String random() {
        final int area = random.nextInt(900) + 100; // 100-999
        final int group = random.nextInt(100); // 00-99
        final int serial = random.nextInt(10000); // 0000-9999
        return String.format("%03d-%02d-%04d", area, group, serial);
    }

    @Override
    public long poolSize() {
        return 900L * 100L * 10000L;
    }

}
