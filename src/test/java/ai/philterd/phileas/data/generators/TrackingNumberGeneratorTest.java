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

public class TrackingNumberGeneratorTest {

    @Test
    public void testGenerateTrackingNumber() {
        final TrackingNumberGenerator generator = new TrackingNumberGenerator(new Random());
        
        boolean foundFedEx = false;
        boolean foundUPS = false;
        
        for (int i = 0; i < 100; i++) {
            final String tracking = generator.random();
            assertNotNull(tracking);
            if (tracking.startsWith("1Z")) {
                foundUPS = true;
            } else if (tracking.matches("\\d{12}")) {
                foundFedEx = true;
            }
        }
        
        assertTrue("Should have generated at least one UPS tracking number", foundUPS);
        assertTrue("Should have generated at least one FedEx tracking number", foundFedEx);
    }

    @Test
    public void testPoolSize() {
        final TrackingNumberGenerator generator = new TrackingNumberGenerator(new Random());
        assertEquals(Long.MAX_VALUE, generator.poolSize());
    }

}
