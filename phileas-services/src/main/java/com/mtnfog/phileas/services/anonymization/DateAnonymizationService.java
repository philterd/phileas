package com.mtnfog.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.DateFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Random;

public class DateAnonymizationService extends AbstractAnonymizationService {

    private static final Logger LOGGER = LogManager.getLogger(DateAnonymizationService.class);

    private Random random;

    public DateAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
        this.random = new Random();
    }

    @Override
    public String anonymize(String token) {

        LocalDate localDate = getRandomDate();

        // Generate a date in the same format as the token.

        if(token.matches(DateFilter.DATE_YYYYMMDD_REGEX.pattern())) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return localDate.format(formatter);

        } else if(token.matches(DateFilter.DATE_MMDDYYYY_REGEX.pattern())) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
            return localDate.format(formatter);

        } else if(token.matches(DateFilter.DATE_MDYYYY_REGEX.pattern())) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M-d-yyyy");
            return localDate.format(formatter);

        } else if(token.matches(DateFilter.DATE_MONTH_REGEX.pattern())) {

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
