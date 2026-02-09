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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;

/**
 * Generates random dates.
 */
public class DateGenerator implements DataGenerator.Generator<String> {
    private final Random random;
    private final LocalDate startDate;
    private final long days;
    private final DateTimeFormatter formatter;

    /**
     * Creates a new date generator.
     * @param random The {@link Random} to use.
     */
    public DateGenerator(final Random random) {
        this(random, 1970, 2030);
    }

    /**
     * Creates a new date generator.
     * @param random The {@link Random} to use.
     * @param minYear The minimum year.
     * @param maxYear The maximum year.
     */
    public DateGenerator(final Random random, final int minYear, final int maxYear) {
        this(random, minYear, maxYear, "yyyy-MM-dd");
    }

    /**
     * Creates a new date generator.
     * @param random The {@link Random} to use.
     * @param minYear The minimum year.
     * @param maxYear The maximum year.
     * @param pattern The date pattern to use.
     */
    public DateGenerator(final Random random, final int minYear, final int maxYear, final String pattern) {
        this(random, minYear, maxYear, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Creates a new date generator.
     * @param random The {@link Random} to use.
     * @param minYear The minimum year.
     * @param maxYear The maximum year.
     * @param formatter The {@link DateTimeFormatter} to use.
     */
    public DateGenerator(final Random random, final int minYear, final int maxYear, final DateTimeFormatter formatter) {
        this.random = random;
        this.startDate = LocalDate.of(minYear, 1, 1);
        final LocalDate endDate = LocalDate.of(maxYear, 1, 1);
        this.days = ChronoUnit.DAYS.between(startDate, endDate);
        this.formatter = formatter;
    }

    @Override
    public String random() {
        return startDate.plusDays(random.nextInt((int) days)).format(formatter);
    }

    @Override
    public long poolSize() {
        return days;
    }

}
