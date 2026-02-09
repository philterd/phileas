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
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class URLGeneratorTest {

    @Test
    public void testGenerateURL() {
        final List<String> firstNamePool = Arrays.asList("John", "Jane", "Mary");
        final DataGenerator.Generator<String> firstNames = new FirstNameGenerator(firstNamePool, new SecureRandom());
        final URLGenerator generator = new URLGenerator(firstNames, new SecureRandom());
        final String url = generator.random();
        assertNotNull(url);
        assertTrue(url.startsWith("http://") || url.startsWith("https://"));
    }

    @Test
    public void testCustomPools() {
        final List<String> firstNamePool = Arrays.asList("John", "Jane", "Mary");
        final DataGenerator.Generator<String> firstNames = new FirstNameGenerator(firstNamePool, new SecureRandom());
        final String[] customProtocols = {"ftp"};
        final String[] customExtensions = {"biz"};
        final URLGenerator urlGenerator = new URLGenerator(firstNames, new SecureRandom(), customProtocols, customExtensions);
        final String url = urlGenerator.random();
        assertTrue(url.startsWith("ftp://"));
        assertTrue(url.endsWith(".biz"));
    }

    @Test
    public void testPoolSize() {
        final List<String> firstNamePool = Arrays.asList("John", "Jane", "Mary");
        final DataGenerator.Generator<String> firstNames = new FirstNameGenerator(firstNamePool, new SecureRandom());
        final URLGenerator generator = new URLGenerator(firstNames, new SecureRandom());
        assertTrue(generator.poolSize() > 0);
    }

}
