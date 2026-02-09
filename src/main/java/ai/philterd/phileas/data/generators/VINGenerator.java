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
 * Generates random Vehicle Identification Numbers (VIN).
 */
public class VINGenerator implements DataGenerator.Generator<String> {
    private final Random random;
    private final String chars = "0123456789ABCDEFGHJKLMNPRSTUVWXYZ";

    /**
     * Creates a new VIN generator.
     * @param random The {@link Random} to use.
     */
    public VINGenerator(final Random random) {
        this.random = random;
    }

    @Override
    public String random() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 17; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public long poolSize() {
        return Long.MAX_VALUE;
    }

}
