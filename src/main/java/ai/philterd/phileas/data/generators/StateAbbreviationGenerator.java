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

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Generates random state abbreviations.
 */
public class StateAbbreviationGenerator implements DataGenerator.Generator<String> {
    private final Random random;
    private final List<String> abbreviations;
    private static final List<String> DEFAULT_ABBREVIATIONS = Arrays.asList("AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY");

    /**
     * Creates a new state abbreviation generator.
     * @param random The {@link Random} to use.
     */
    public StateAbbreviationGenerator(final Random random) {
        this(random, DEFAULT_ABBREVIATIONS);
    }

    /**
     * Creates a new state abbreviation generator.
     * @param random The {@link Random} to use.
     * @param abbreviations A list of state abbreviations.
     */
    public StateAbbreviationGenerator(final Random random, final List<String> abbreviations) {
        this.random = random;
        this.abbreviations = abbreviations;
    }

    @Override
    public String random() {
        return abbreviations.get(random.nextInt(abbreviations.size()));
    }

    @Override
    public long poolSize() {
        return abbreviations.size();
    }

}
