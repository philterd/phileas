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

import java.time.LocalDate;
import java.util.Random;

import static org.junit.Assert.*;

public class DateGeneratorTest {

    @Test
    public void testGenerateDate() {
        final DateGenerator generator = new DateGenerator(new Random());
        final String date = generator.random();
        assertNotNull(date);
        assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testDateGeneratorDefaultConstructor() {
        final DateGenerator generator = new DateGenerator(new Random());
        assertNotNull(generator.random());
        assertTrue(generator.poolSize() > 0);
    }

    @Test
    public void testGenerateDateWithBounds() {
        final int minYear = 2000;
        final int maxYear = 2010;
        final DateGenerator boundedGenerator = new DateGenerator(new Random(), minYear, maxYear);
        
        for (int i = 0; i < 100; i++) {
            final String dateStr = boundedGenerator.random();
            final LocalDate date = LocalDate.parse(dateStr);
            assertTrue("Year " + date.getYear() + " should be >= " + minYear, date.getYear() >= minYear);
            assertTrue("Year " + date.getYear() + " should be < " + maxYear, date.getYear() < maxYear);
        }
        assertTrue(boundedGenerator.poolSize() >= (maxYear - minYear) * 365L);
    }

    @Test
    public void testPoolSize() {
        final DateGenerator generator = new DateGenerator(new Random());
        assertTrue(generator.poolSize() >= 60L * 365L);
    }

    @Test
    public void testLeapYearDate() {
        // 2024 is a leap year.
        final DateGenerator generator = new DateGenerator(new Random(), 2024, 2025);
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
        assertTrue("Feb 29 should be possible in a leap year", foundFeb29);
        assertTrue("Dec 31 should be possible in a leap year", foundDec31);
    }

    @Test
    public void testNonLeapYearDate() {
        // 2023 is not a leap year.
        final DateGenerator generator = new DateGenerator(new Random(), 2023, 2024);
        for (int i = 0; i < 10000; i++) {
            final String dateStr = generator.random();
            assertFalse("Feb 29 should not be possible in a non-leap year: " + dateStr, dateStr.endsWith("-02-29"));
            assertFalse("April 31 is never valid: " + dateStr, dateStr.endsWith("-04-31"));
        }
    }

    @Test
    public void testCustomDateFormat() {
        final String pattern = "MM/dd/yyyy";
        final DateGenerator generator = new DateGenerator(new Random(), 2020, 2021, pattern);
        final String date = generator.random();
        assertNotNull(date);
        assertTrue(date.matches("\\d{2}/\\d{2}/\\d{4}"));
    }

}
