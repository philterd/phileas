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
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DateGeneratorTest {

    @Test
    public void testGenerateDate() {
        final DateGenerator generator = new DateGenerator(new SecureRandom());
        final String date = generator.random();
        assertNotNull(date);
        assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testDateGeneratorDefaultConstructor() {
        final DateGenerator generator = new DateGenerator(new SecureRandom());
        assertNotNull(generator.random());
        assertTrue(generator.poolSize() > 0);
    }

    @Test
    public void testGenerateDateWithBounds() {
        final int minYear = 2000;
        final int maxYear = 2010;
        final DateGenerator boundedGenerator = new DateGenerator(new SecureRandom(), minYear, maxYear);
        
        for (int i = 0; i < 100; i++) {
            final String dateStr = boundedGenerator.random();
            final LocalDate date = LocalDate.parse(dateStr);
            assertTrue(date.getYear() >= minYear, "Year " + date.getYear() + " should be >= " + minYear);
            assertTrue(date.getYear() < maxYear, "Year " + date.getYear() + " should be < " + maxYear);
        }
        assertTrue(boundedGenerator.poolSize() >= (maxYear - minYear) * 365L);
    }

    @Test
    public void testPoolSize() {
        final DateGenerator generator = new DateGenerator(new SecureRandom());
        assertTrue(generator.poolSize() >= 60L * 365L);
    }

    @Test
    public void testLeapYearDate() {
        // 2024 is a leap year.
        final DateGenerator generator = new DateGenerator(new SecureRandom(), 2024, 2025);
        boolean foundFeb29 = false;
        boolean foundDec31 = false;
        for (int i = 0; i < 10000; i++) {
            final String dateStr = generator.random();
            if (dateStr.endsWith("-02-29")) {
                foundFeb29 = true;
            }
            if (dateStr.endsWith("-12-31")) {
                foundDec31 = true;
            }
        }
        assertTrue(foundFeb29, "Feb 29 should be possible in a leap year");
        assertTrue(foundDec31, "Dec 31 should be possible in a leap year");
    }

    @Test
    public void testNonLeapYearDate() {
        // 2023 is not a leap year.
        final DateGenerator generator = new DateGenerator(new SecureRandom(), 2023, 2024);
        for (int i = 0; i < 10000; i++) {
            final String dateStr = generator.random();
            assertFalse(dateStr.endsWith("-02-29"), "Feb 29 should not be possible in a non-leap year: " + dateStr);
            assertFalse(dateStr.endsWith("-04-31"), "April 31 is never valid: " + dateStr);
        }
    }

    @Test
    public void testCustomDateFormat() {
        final String pattern = "MM/dd/yyyy";
        final DateGenerator generator = new DateGenerator(new SecureRandom(), 2020, 2021, pattern);
        final String date = generator.random();
        assertNotNull(date);
        assertTrue(date.matches("\\d{2}/\\d{2}/\\d{4}"));
    }

}
