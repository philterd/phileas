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
 * Generates random state names.
 */
public class StateGenerator implements DataGenerator.Generator<String> {
    private final Random random;
    private final List<String> states;
    private static final List<String> DEFAULT_STATES = Arrays.asList("Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming");

    /**
     * Creates a new state generator.
     * @param random The {@link Random} to use.
     */
    public StateGenerator(final Random random) {
        this(random, DEFAULT_STATES);
    }

    /**
     * Creates a new state generator.
     * @param random The {@link Random} to use.
     * @param states A list of state names.
     */
    public StateGenerator(final Random random, final List<String> states) {
        this.random = random;
        this.states = states;
    }

    @Override
    public String random() {
        return states.get(random.nextInt(states.size()));
    }

    @Override
    public long poolSize() {
        return states.size();
    }

}
