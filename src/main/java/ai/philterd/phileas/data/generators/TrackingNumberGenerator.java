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
 * Generates random tracking numbers (UPS and FedEx).
 */
public class TrackingNumberGenerator implements DataGenerator.Generator<String> {
    private final Random random;

    /**
     * Creates a new tracking number generator.
     * @param random The {@link Random} to use.
     */
    public TrackingNumberGenerator(final Random random) {
        this.random = random;
    }

    @Override
    public String random() {
        if (random.nextBoolean()) {
            return generateFedEx();
        } else {
            return generateUPS();
        }
    }

    private String generateFedEx() {
        // FedEx Ground: 12 digits
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String generateUPS() {
        // UPS: 1Z + 6 characters (sender ID) + 2 characters (service level) + 8 characters (package ID) + 1 check digit
        // Simplified: 1Z + 16 alphanumeric characters
        final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final StringBuilder sb = new StringBuilder("1Z");
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public long poolSize() {
        // Very large pool
        return Long.MAX_VALUE;
    }

}
