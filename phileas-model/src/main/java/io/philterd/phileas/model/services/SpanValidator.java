package io.philterd.phileas.model.services;

import io.philterd.phileas.model.objects.Span;

public interface SpanValidator {

    boolean validate(Span span);

}
