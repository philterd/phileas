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

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Generates random hospital abbreviations.
 */
public class HospitalAbbreviationGenerator extends AbstractGenerator<String> {

    private final Random random;
    private final List<String> hospitals;

    /**
     * Creates a new hospital abbreviation generator.
     * @throws IOException if the hospital data cannot be loaded.
     */
    public HospitalAbbreviationGenerator() throws IOException {
        this(new Random());
    }

    /**
     * Creates a new hospital abbreviation generator.
     * @param random The {@link Random} to use.
     * @throws IOException if the hospital data cannot be loaded.
     */
    public HospitalAbbreviationGenerator(final Random random) throws IOException {
        this.random = random;
        this.hospitals = loadNames("/hospitals.txt");
    }

    /**
     * Creates a new hospital abbreviation generator.
     * @param hospitals A list of hospital names.
     * @param random The {@link Random} to use.
     */
    public HospitalAbbreviationGenerator(final List<String> hospitals, final Random random) {
        this.random = random;
        this.hospitals = hospitals;
    }

    @Override
    public String random() {
        final String hospital = hospitals.get(random.nextInt(hospitals.size()));
        return abbreviate(hospital);
    }

    @Override
    public long poolSize() {
        return hospitals.size();
    }

    private String abbreviate(final String name) {
        final StringBuilder abbreviation = new StringBuilder();
        final String[] parts = name.split("\\s+");
        for (final String part : parts) {
            if (!part.isEmpty()) {
                abbreviation.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return abbreviation.toString();
    }

}
