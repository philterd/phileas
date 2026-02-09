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

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class IPAddressGeneratorTest {

    @Test
    public void testGenerateIPAddress() {
        final IPAddressGenerator generator = new IPAddressGenerator(new Random());
        final String ip = generator.random();
        assertNotNull(ip);
        assertTrue(ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"));
    }

    @Test
    public void testPoolSize() {
        final IPAddressGenerator generator = new IPAddressGenerator(new Random());
        assertEquals(4294967296L, generator.poolSize());
    }

}
