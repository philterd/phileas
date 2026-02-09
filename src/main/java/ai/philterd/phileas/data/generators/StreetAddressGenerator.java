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
 * Generates random street addresses.
 */
public class StreetAddressGenerator extends AbstractGenerator<String> {

    private final DataGenerator.Generator<String> surnames;
    private final Random random;
    private final String[] suffixes = {"St", "Ave", "Blvd", "Rd", "Ln", "Dr", "Ct", "Pl", "Way", "Ter"};

    /**
     * Creates a new street address generator.
     * @param surnames A generator for surnames to be used as street names.
     * @param random The {@link Random} to use.
     */
    public StreetAddressGenerator(final DataGenerator.Generator<String> surnames, final Random random) {
        this.surnames = surnames;
        this.random = random;
    }

    @Override
    public String random() {
        final int houseNumber = random.nextInt(9999) + 1;
        final String streetName = surnames.random();
        final String suffix = suffixes[random.nextInt(suffixes.length)];
        return houseNumber + " " + streetName + " " + suffix;
    }

    @Override
    public long poolSize() {
        return 9999 * surnames.poolSize() * suffixes.length;
    }

}
