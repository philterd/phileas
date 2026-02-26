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

import java.util.List;
import java.util.Random;

/**
 * Generates random first names.
 */
public class FirstNameGenerator extends AbstractGenerator<String> {
    private final List<String> firstNames;
    private final Random random;

    /**
     * Creates a new first name generator.
     * @param firstNames A list of first names.
     * @param random The {@link Random} to use.
     */
    public FirstNameGenerator(final List<String> firstNames, final Random random) {
        this.firstNames = firstNames;
        this.random = random;
    }

    @Override
    public String random() {
        return firstNames.get(random.nextInt(firstNames.size()));
    }

    @Override
    public long poolSize() {
        return firstNames.size();
    }

}
