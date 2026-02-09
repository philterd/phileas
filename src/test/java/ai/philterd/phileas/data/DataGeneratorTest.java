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
package ai.philterd.phileas.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for DefaultDataGenerator.
 */
public class DataGeneratorTest {

    private DataGenerator generator;

    @BeforeEach
    public void setUp() throws Exception {
        generator = new DefaultDataGenerator();
    }

    @Test
    public void testPoolSizes() {
        assertTrue(generator.firstNames().poolSize() > 0);
        assertTrue(generator.surnames().poolSize() > 0);
        assertTrue(generator.fullNames().poolSize() > 0);
        assertEquals(900000000L, generator.ssn().poolSize());
        assertEquals(8100000000L, generator.phoneNumbers().poolSize());
        assertTrue(generator.emailAddresses().poolSize() > 0);
        assertEquals(101L, generator.age().poolSize());
        assertEquals(1000000000L, generator.bankRoutingNumbers().poolSize());
        assertEquals(10000L * 10000L * 10000L * 10000L, generator.creditCardNumbers().poolSize());
        assertTrue(generator.dates().poolSize() >= 60L * 365L);
        assertEquals(Long.MAX_VALUE, generator.iban().poolSize());
        assertEquals(4294967296L, generator.ipAddresses().poolSize());
        assertEquals(281474976710656L, generator.macAddresses().poolSize());
        assertEquals(2600000000L, generator.passportNumbers().poolSize());
        assertEquals(50L, generator.states().poolSize());
        assertEquals(50L, generator.stateAbbreviations().poolSize());
        assertEquals(100000L, generator.zipCodes().poolSize());
        assertEquals(Long.MAX_VALUE, generator.bitcoinAddresses().poolSize());
        assertEquals(Long.MAX_VALUE, generator.vin().poolSize());
        assertTrue(generator.urls().poolSize() > 0);
        assertEquals(1000000000L, generator.driversLicenseNumbers().poolSize());
        assertTrue(generator.hospitals().poolSize() > 0);
        assertTrue(generator.hospitalAbbreviations().poolSize() > 0);
        assertEquals(Long.MAX_VALUE, generator.trackingNumbers().poolSize());
        assertEquals(18720L, generator.cities().poolSize());
        assertTrue(generator.counties().poolSize() > 0);
        assertEquals(1000L, generator.customId("123").poolSize());
    }

    @Test
    public void testGeneratorInterface() {
        final DataGenerator.Generator<String> firstNameGenerator = generator.firstNames();
        assertNotNull(firstNameGenerator.random());
        assertTrue(firstNameGenerator.poolSize() > 0);

        final DataGenerator.Generator<String> ssnGenerator = generator.ssn();
        assertNotNull(ssnGenerator.random());
        assertEquals(900000000L, ssnGenerator.poolSize());

        final DataGenerator.Generator<Integer> ageGenerator = generator.age();
        assertNotNull(ageGenerator.random());
        assertEquals(101L, ageGenerator.poolSize());
    }

    @Test
    public void testFieldAccess() {
        if (generator instanceof DefaultDataGenerator defaultGenerator) {
            assertNotNull(defaultGenerator.firstNames.random());
            assertTrue(defaultGenerator.firstNames.poolSize() > 0);
            
            assertNotNull(defaultGenerator.ssn.random());
            assertEquals(900000000L, defaultGenerator.ssn.poolSize());
        }
    }

    @Test
    public void testMultipleGenerations() {
        // Generate multiple instances to ensure randomness
        final String name1 = generator.fullNames().random();
        final String name2 = generator.fullNames().random();
        final String ssn1 = generator.ssn().random();
        final String ssn2 = generator.ssn().random();
        
        // Just verify they are all valid
        assertNotNull(name1);
        assertNotNull(name2);
        assertNotNull(ssn1);
        assertNotNull(ssn2);
    }

    @Test
    public void testCustomRandom() throws Exception {
        final java.util.Random random1 = new java.util.Random(12345);
        final DataGenerator generator1 = new DefaultDataGenerator(random1);

        final java.util.Random random2 = new java.util.Random(12345);
        final DataGenerator generator2 = new DefaultDataGenerator(random2);

        assertEquals(generator1.ssn().random(), generator2.ssn().random());
        assertEquals(generator1.firstNames().random(), generator2.firstNames().random());
    }

    @Test
    public void testDefaultDataGeneratorPoolSize() {
        if (generator instanceof DefaultDataGenerator) {
            assertEquals(0, ((DefaultDataGenerator) generator).poolSize());
        }
    }

    @Test
    public void testDefaultDataGeneratorRandom() {
        if (generator instanceof DefaultDataGenerator) {
            assertNull(((DefaultDataGenerator) generator).random());
        }
    }

    @Test
    public void testDatesWithPattern() {
        final String pattern = "MM-dd-yyyy";
        final String date = generator.dates(pattern).random();
        assertNotNull(date);
        assertTrue(date.matches("\\d{2}-\\d{2}-\\d{4}"));
    }

}
