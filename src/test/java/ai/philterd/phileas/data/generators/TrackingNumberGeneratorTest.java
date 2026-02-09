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

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrackingNumberGeneratorTest {

    @Test
    public void testGenerateTrackingNumber() {
        final TrackingNumberGenerator generator = new TrackingNumberGenerator(new SecureRandom());
        
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
        
        assertTrue(foundUPS, "Should have generated at least one UPS tracking number");
        assertTrue(foundFedEx, "Should have generated at least one FedEx tracking number");
    }

    @Test
    public void testPoolSize() {
        final TrackingNumberGenerator generator = new TrackingNumberGenerator(new SecureRandom());
        assertEquals(Long.MAX_VALUE, generator.poolSize());
    }

}
