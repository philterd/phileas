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
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

/**
 * Generates random hospital names.
 */
public class HospitalGenerator extends AbstractGenerator<String> {

    private final Random random;
    private final List<String> hospitals;

    /**
     * Creates a new hospital generator.
     * @throws IOException if the hospital data cannot be loaded.
     */
    public HospitalGenerator() throws IOException {
        this(new SecureRandom());
    }

    /**
     * Creates a new hospital generator.
     * @param random The {@link Random} to use.
     * @throws IOException if the hospital data cannot be loaded.
     */
    public HospitalGenerator(final Random random) throws IOException {
        this.random = random;
        this.hospitals = loadNames("/hospitals.txt");
    }

    /**
     * Creates a new hospital generator.
     * @param hospitals A list of hospital names.
     * @param random The {@link Random} to use.
     */
    public HospitalGenerator(final List<String> hospitals, final Random random) {
        this.random = random;
        this.hospitals = hospitals;
    }

    @Override
    public String random() {
        return hospitals.get(random.nextInt(hospitals.size()));
    }

    @Override
    public long poolSize() {
        return hospitals.size();
    }

}
