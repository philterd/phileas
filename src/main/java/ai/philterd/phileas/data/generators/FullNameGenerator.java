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

/**
 * Generates random full names.
 */
public class FullNameGenerator implements DataGenerator.Generator<String> {
    private final DataGenerator.Generator<String> firstNames;
    private final DataGenerator.Generator<String> surnames;

    /**
     * Creates a new full name generator.
     * @param firstNames A first name generator.
     * @param surnames A surname generator.
     */
    public FullNameGenerator(final DataGenerator.Generator<String> firstNames, final DataGenerator.Generator<String> surnames) {
        this.firstNames = firstNames;
        this.surnames = surnames;
    }

    @Override
    public String random() {
        return firstNames.random() + " " + surnames.random();
    }

    @Override
    public long poolSize() {
        return firstNames.poolSize() * surnames.poolSize();
    }

}
