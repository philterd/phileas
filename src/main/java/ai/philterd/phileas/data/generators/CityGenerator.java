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
 * Generates random city names.
 */
public class CityGenerator extends AbstractGenerator<String> {
    private final List<String> cities;
    private final Random random;

    /**
     * Creates a new city generator.
     * @param cities A list of cities.
     * @param random The {@link Random} to use.
     */
    public CityGenerator(final List<String> cities, final Random random) {
        this.cities = cities;
        this.random = random;
    }

    @Override
    public String random() {
        return cities.get(random.nextInt(cities.size()));
    }

    @Override
    public long poolSize() {
        return cities.size();
    }

}
