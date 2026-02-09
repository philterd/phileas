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
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmailAddressGeneratorTest {

    @Test
    public void testGenerateEmail() {
        final List<String> firstNamePool = Arrays.asList("John", "Jane", "Mary");
        final List<String> surnamePool = Arrays.asList("Doe", "Smith", "Jones");
        final DataGenerator.Generator<String> firstNames = new FirstNameGenerator(firstNamePool, new SecureRandom());
        final DataGenerator.Generator<String> surnames = new SurnameGenerator(surnamePool, new SecureRandom());
        final EmailAddressGenerator generator = new EmailAddressGenerator(firstNames, surnames, new SecureRandom());
        
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
        final List<String> firstNamePool = Arrays.asList("John", "Jane", "Mary");
        final List<String> surnamePool = Arrays.asList("Doe", "Smith", "Jones");
        final DataGenerator.Generator<String> firstNames = new FirstNameGenerator(firstNamePool, new SecureRandom());
        final DataGenerator.Generator<String> surnames = new SurnameGenerator(surnamePool, new SecureRandom());
        final String[] customDomains = {"test.com"};
        final EmailAddressGenerator emailGenerator = new EmailAddressGenerator(firstNames, surnames, new SecureRandom(), customDomains);
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
        final List<String> firstNamePool = Arrays.asList("John", "Jane", "Mary");
        final List<String> surnamePool = Arrays.asList("Doe", "Smith", "Jones");
        final DataGenerator.Generator<String> firstNames = new FirstNameGenerator(firstNamePool, new SecureRandom());
        final DataGenerator.Generator<String> surnames = new SurnameGenerator(surnamePool, new SecureRandom());
        final String[] domains = {"gmail.com", "yahoo.com"};
        final EmailAddressGenerator generator = new EmailAddressGenerator(firstNames, surnames, new SecureRandom(), domains);
        assertEquals(3 * 3 * 1000L * 2, generator.poolSize());
    }

}
