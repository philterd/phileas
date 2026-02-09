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
 * Generates random URLs.
 */
public class URLGenerator implements DataGenerator.Generator<String> {
    private final DataGenerator.Generator<String> firstNames;
    private final Random random;
    private final String[] protocols;
    private final String[] extensions;
    private static final String[] DEFAULT_PROTOCOLS = {"http", "https"};
    private static final String[] DEFAULT_EXTENSIONS = {"com", "org", "net", "io", "gov"};

    /**
     * Creates a new URL generator.
     * @param firstNames A generator for first names.
     * @param random The {@link Random} to use.
     */
    public URLGenerator(final DataGenerator.Generator<String> firstNames, final Random random) {
        this(firstNames, random, DEFAULT_PROTOCOLS, DEFAULT_EXTENSIONS);
    }

    /**
     * Creates a new URL generator.
     * @param firstNames A generator for first names.
     * @param random The {@link Random} to use.
     * @param protocols A list of protocols.
     * @param extensions A list of extensions.
     */
    public URLGenerator(final DataGenerator.Generator<String> firstNames, final Random random, final String[] protocols, final String[] extensions) {
        this.firstNames = firstNames;
        this.random = random;
        this.protocols = protocols;
        this.extensions = extensions;
    }

    @Override
    public String random() {
        final String domain = firstNames.random().toLowerCase() + random.nextInt(100);
        return protocols[random.nextInt(protocols.length)] + "://www." + domain + "." + extensions[random.nextInt(extensions.length)];
    }

    @Override
    public long poolSize() {
        return protocols.length * firstNames.poolSize() * 100L * extensions.length;
    }

}
