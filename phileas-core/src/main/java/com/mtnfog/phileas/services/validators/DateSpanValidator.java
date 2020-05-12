package com.mtnfog.phileas.services.validators;

import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.SpanValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public class DateSpanValidator implements SpanValidator {

    private static final Logger LOGGER = LogManager.getLogger(DateSpanValidator.class);

    public static SpanValidator getInstance() {
        return new DateSpanValidator();
    }

    @Override
    public boolean validate(Span span) {

        boolean valid = true;

        try {

            LocalDate.parse(span.getText(), DateTimeFormatter.ofPattern(span.getPattern()).withResolverStyle(ResolverStyle.STRICT));

        } catch (DateTimeException ex) {
            // Not a date.
            valid = false;
            LOGGER.error(ex.getMessage());
        }

        LOGGER.debug("Validated date span {} against pattern {}: Valid = {}", span.getText(), span.getPattern(), valid);

        return valid;

    }

}
