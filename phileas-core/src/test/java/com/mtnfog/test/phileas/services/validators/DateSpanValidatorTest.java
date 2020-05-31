package com.mtnfog.test.phileas.services.validators;

import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.SpanValidator;
import com.mtnfog.phileas.services.validators.DateSpanValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateSpanValidatorTest {

    @Test
    public void test1() {

        // https://stackoverflow.com/a/27454146/1428388
        // https://stackoverflow.com/a/29014580/1428388

        final Span span = new Span();
        span.setPattern("MM-dd-uuuu");
        span.setText("05-20-2020");

        final SpanValidator spanValidator = DateSpanValidator.getInstance();
        Assertions.assertTrue(spanValidator.validate(span));

    }

    @Test
    public void test2() {

        final Span span = new Span();
        span.setPattern("MM-dd-uuuu");
        span.setText("15-20-2020");

        final SpanValidator spanValidator = DateSpanValidator.getInstance();
        Assertions.assertFalse(spanValidator.validate(span));

    }

}
