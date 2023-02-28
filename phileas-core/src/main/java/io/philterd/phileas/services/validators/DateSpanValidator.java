package io.philterd.phileas.services.validators;

import io.philterd.phileas.model.objects.Span;
import io.philterd.phileas.model.services.SpanValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

public class DateSpanValidator implements SpanValidator {

    private static final Logger LOGGER = LogManager.getLogger(DateSpanValidator.class);

    private static SpanValidator spanValidator;

    public static SpanValidator getInstance() {

        if(spanValidator == null) {
            spanValidator = new DateSpanValidator();
        }

        return spanValidator;
    }

    private DateSpanValidator() {
        // Use the static getInstance().
    }

    @Override
    public boolean validate(Span span) {

        boolean valid;

        try {

            LOGGER.info("Date {} : Pattern {}", span.getText(), span.getPattern());

            final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(span.getPattern(), Locale.US).withResolverStyle(ResolverStyle.STRICT);
            final LocalDateTime localDateTime = LocalDate.parse(span.getText(), dtf).atStartOfDay();

            // If it's a 2 digit year add 2000.
            final int length = String.valueOf(localDateTime.getYear()).length();
            int year = localDateTime.getYear();
            if(length == 2) {
                year += + 2000;
            }

            // Sanity check on the year. It should be greater than 1800 and less than 2200.
            if(year >= 1800 && year <= 2200) {
                valid = true;
            } else {
                valid = false;
            }

        } catch (DateTimeException ex) {
            // Not a date.
            valid = false;
            LOGGER.error(ex.getMessage());
        }

        LOGGER.debug("Validated date span {} against pattern {}: Valid = {}", span.getText(), span.getPattern(), valid);

        return valid;

    }

}
