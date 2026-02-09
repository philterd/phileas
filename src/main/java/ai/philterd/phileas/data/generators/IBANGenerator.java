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
 * Generates random IBANs.
 */
public class IBANGenerator implements DataGenerator.Generator<String> {
    private final Random random;

    /**
     * Creates a new IBAN generator.
     * @param random The {@link Random} to use.
     */
    public IBANGenerator(final Random random) {
        this.random = random;
    }

    @Override
    public String random() {
        final String country = "US";
        final String checkDigits = String.format("%02d", random.nextInt(100));
        final String bban = String.format("%020d", (long) (random.nextDouble() * 1e20));
        return country + checkDigits + bban;
    }

    @Override
    public long poolSize() {
        return Long.MAX_VALUE;
    }

}
