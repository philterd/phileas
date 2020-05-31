package com.mtnfog.phileas.services.validators;

import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.SpanValidator;
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

        boolean valid = true;

        try {

            final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(span.getPattern(), Locale.US).withResolverStyle(ResolverStyle.STRICT);
            LocalDateTime localDateTime = LocalDate.parse(span.getText(), dtf).atStartOfDay();

        } catch (DateTimeException ex) {
            // Not a date.
            valid = false;
            LOGGER.error(ex.getMessage());
        }

        LOGGER.debug("Validated date span {} against pattern {}: Valid = {}", span.getText(), span.getPattern(), valid);

        return valid;

    }

}
