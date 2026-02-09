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

import java.io.IOException;
import java.util.Collections;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class EmailAddressGeneratorTest {

    @Test
    public void testGenerateEmail() {
        final DataGenerator.Generator<String> firstNames = new FirstNameGenerator(Collections.singletonList("John"), new Random());
        final DataGenerator.Generator<String> surnames = new SurnameGenerator(Collections.singletonList("Doe"), new Random());
        final EmailAddressGenerator generator = new EmailAddressGenerator(firstNames, surnames, new Random());
        
        final String email = generator.random();
        assertNotNull(email, "Email should not be null");
        assertTrue(email.contains("@"), "Email should contain @");
        assertTrue(email.contains("."), "Email should contain .");
        final String[] parts = email.split("@");
        assertEquals(2, parts.length, "Email should have two parts separated by @");
        assertTrue(parts[0].matches(".*\\d{3}"), "Username should end with 3 digits");
    }

    @Test
    public void testCustomPools() {
        final DataGenerator.Generator<String> firstNames = new FirstNameGenerator(Collections.singletonList("John"), new Random());
        final DataGenerator.Generator<String> surnames = new SurnameGenerator(Collections.singletonList("Doe"), new Random());
        final String[] customDomains = {"test.com"};
        final EmailAddressGenerator emailGenerator = new EmailAddressGenerator(firstNames, surnames, new Random(), customDomains);
        assertTrue(emailGenerator.random().endsWith("@test.com"));
    }

    @Test
    public void testEmailAddressGeneratorDefaultConstructor() throws IOException {
        final EmailAddressGenerator emailGenerator = new EmailAddressGenerator();
        final String email = emailGenerator.random();
        assertNotNull(email);
        assertTrue(email.contains("@"));
        assertTrue(email.contains("."));
    }

    @Test
    public void testPoolSize() {
        final DataGenerator.Generator<String> firstNames = new FirstNameGenerator(Collections.singletonList("John"), new Random());
        final DataGenerator.Generator<String> surnames = new SurnameGenerator(Collections.singletonList("Doe"), new Random());
        final String[] domains = {"gmail.com", "yahoo.com"};
        final EmailAddressGenerator generator = new EmailAddressGenerator(firstNames, surnames, new Random(), domains);
        assertEquals(1 * 1 * 1000L * 2, generator.poolSize());
    }

}
