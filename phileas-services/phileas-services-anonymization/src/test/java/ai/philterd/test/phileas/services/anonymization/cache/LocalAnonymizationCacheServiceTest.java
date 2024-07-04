/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.test.phileas.services.anonymization.cache;

import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocalAnonymizationCacheServiceTest {

    @Test
    public void putAndContains() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        cache.put("context", "k", "v");

        Assertions.assertTrue(cache.contains("context", "k"));

    }

    @Test
    public void containsValue() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        cache.put("context", "k", "v");

        Assertions.assertTrue(cache.containsValue("context", "v"));
        Assertions.assertFalse(cache.containsValue("context", "k"));

    }

    @Test
    public void getAndPut() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assertions.assertEquals("v", value);

        final String value2 = cache.get("context", "doesnotexist");
        Assertions.assertNull(value2);

    }

    @Test
    public void putAndRemove() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        cache.put("context", "k", "v");
        final String value = cache.get("context", "k");
        Assertions.assertEquals("v", value);

        cache.remove("context", "k");
        final String value2 = cache.get("context", "k");
        Assertions.assertNull(value2);

    }

    @Test
    public void generateKey() {

        final LocalAnonymizationCacheService cache = new LocalAnonymizationCacheService();

        final String hash = cache.generateKey("context", "k");

        Assertions.assertTrue(hash.matches("^[a-f0-9]{32}$"));
        Assertions.assertEquals("84e86fe7599f42d95d8ef20375b5a66e", hash);

    }

}