/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.anonymization;

import ai.philterd.phileas.services.context.ContextService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Random;
import java.util.regex.Pattern;

public class DateAnonymizationService extends AbstractAnonymizationService {

    private static final Logger LOGGER = LogManager.getLogger(DateAnonymizationService.class);

    // TODO: Don't duplicate these from DateFilter.
    private static final Pattern DATE_YYYYMMDD_REGEX = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");
    private static final Pattern DATE_MMDDYYYY_REGEX = Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}\\b");
    private static final Pattern DATE_MDYYYY_REGEX = Pattern.compile("\\b\\d{1,2}-\\d{1,2}-\\d{2,4}\\b");
    private static final Pattern DATE_MONTH_REGEX = Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)\\D?(\\d{1,2}(\\D?(st|nd|rd|th))?\\D?)(\\D?((19[7-9]\\d|20\\d{2})|\\d{2}))?\\b", Pattern.CASE_INSENSITIVE);

    public DateAnonymizationService(final ContextService contextService, final Random random) {
        super(contextService, random);
    }

    public DateAnonymizationService(final ContextService contextService) {
        super(contextService);
    }

    @Override
    public ContextService getContextService() {
        return contextService;
    }

    @Override
    public String anonymize(final String token) {

        final LocalDate localDate = getRandomDate();

        // Generate a date in the same format as the token.

        if(token.matches(DATE_YYYYMMDD_REGEX.pattern())) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return localDate.format(formatter);

        } else if(token.matches(DATE_MMDDYYYY_REGEX.pattern())) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
            return localDate.format(formatter);

        } else if(token.matches(DATE_MDYYYY_REGEX.pattern())) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M-d-yyyy");
            return localDate.format(formatter);

        } else if(token.matches(DATE_MONTH_REGEX.pattern())) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
            return localDate.format(formatter);

        } else {

            LOGGER.warn("Date {} matched no pattern.", token);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return localDate.format(formatter);

        }

    }

    private LocalDate getRandomDate() {

        int minDay = (int) LocalDate.of(1900, 1, 1).toEpochDay();
        int maxDay = (int) LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), 1, 1).toEpochDay();
        int randomDay = minDay + random.nextInt(maxDay - minDay);

        return LocalDate.ofEpochDay(randomDay);

    }

}
