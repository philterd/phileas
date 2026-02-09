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
import org.junit.Test;

import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.*;

public class URLGeneratorTest {

    @Test
    public void testGenerateURL() {
        final DataGenerator.Generator<String> firstNames = new FirstNameGenerator(Collections.singletonList("John"), new Random());
        final URLGenerator generator = new URLGenerator(firstNames, new Random());
        final String url = generator.random();
        assertNotNull(url);
        assertTrue(url.startsWith("http://") || url.startsWith("https://"));
    }

    @Test
    public void testCustomPools() {
        final DataGenerator.Generator<String> firstNames = new FirstNameGenerator(Collections.singletonList("John"), new Random());
        final String[] customProtocols = {"ftp"};
        final String[] customExtensions = {"biz"};
        final URLGenerator urlGenerator = new URLGenerator(firstNames, new Random(), customProtocols, customExtensions);
        final String url = urlGenerator.random();
        assertTrue(url.startsWith("ftp://"));
        assertTrue(url.endsWith(".biz"));
    }

    @Test
    public void testPoolSize() {
        final DataGenerator.Generator<String> firstNames = new FirstNameGenerator(Collections.singletonList("John"), new Random());
        final URLGenerator generator = new URLGenerator(firstNames, new Random());
        assertTrue(generator.poolSize() > 0);
    }

}
