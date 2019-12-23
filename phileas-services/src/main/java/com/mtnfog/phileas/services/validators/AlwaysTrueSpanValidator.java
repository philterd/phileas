package com.mtnfog.phileas.services.validators;

import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.SpanValidator;

public class AlwaysTrueSpanValidator implements SpanValidator {

    public static SpanValidator getInstance() {
        return new AlwaysTrueSpanValidator();
    }

    @Override
    public boolean validate(Span span) {
        return false;
    }

}
