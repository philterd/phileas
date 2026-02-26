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
 * Generates random ages.
 */
public class AgeGenerator implements DataGenerator.Generator<Integer> {
    private final Random random;

    /**
     * Creates a new age generator.
     * @param random The {@link Random} to use.
     */
    public AgeGenerator(final Random random) {
        this.random = random;
    }

    @Override
    public Integer random() {
        return random.nextInt(101);
    }

    @Override
    public long poolSize() {
        return 101L;
    }

}
