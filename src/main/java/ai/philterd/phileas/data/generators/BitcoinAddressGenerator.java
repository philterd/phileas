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
 * Generates random Bitcoin addresses.
 */
public class BitcoinAddressGenerator implements DataGenerator.Generator<String> {
    private final Random random;
    private final String chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    /**
     * Creates a new Bitcoin address generator.
     * @param random The {@link Random} to use.
     */
    public BitcoinAddressGenerator(final Random random) {
        this.random = random;
    }

    @Override
    public String random() {
        final StringBuilder sb = new StringBuilder("1");
        for (int i = 0; i < 33; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public long poolSize() {
        return Long.MAX_VALUE;
    }

}
