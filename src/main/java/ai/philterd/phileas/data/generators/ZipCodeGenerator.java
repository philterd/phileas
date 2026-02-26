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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates random zip codes.
 */
public class ZipCodeGenerator extends AbstractGenerator<String> implements DataGenerator.Generator<String> {
    private final Random random;
    private final boolean onlyValid;
    private List<String> validZipCodes;

    /**
     * Creates a new zip code generator.
     * @param random The {@link Random} to use.
     */
    public ZipCodeGenerator(final Random random) {
        this(random, false);
    }

    /**
     * Creates a new zip code generator.
     * @param random The {@link Random} to use.
     * @param onlyValid If <code>true</code>, only valid zip codes from the census will be used.
     */
    public ZipCodeGenerator(final Random random, final boolean onlyValid) {
        this.random = random;
        this.onlyValid = onlyValid;

        if (onlyValid) {
            try {
                this.validZipCodes = new ArrayList<>();
                final List<String> lines = loadNames("/zip-code-population.csv");
                for (final String line : lines) {
                    if (!line.startsWith("#")) {
                        final String[] parts = line.split(",");
                        validZipCodes.add(parts[0]);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to load zip code data file.", e);
            }
        }
    }

    @Override
    public String random() {
        if (onlyValid) {
            return validZipCodes.get(random.nextInt(validZipCodes.size()));
        } else {
            return String.format("%05d", random.nextInt(100000));
        }
    }

    @Override
    public long poolSize() {
        if (onlyValid) {
            return validZipCodes.size();
        } else {
            return 100000L;
        }
    }

}
