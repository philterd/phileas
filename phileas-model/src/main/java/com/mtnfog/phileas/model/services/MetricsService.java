package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.objects.Span;

import java.io.Serializable;

public interface MetricsService extends Serializable {

    void incrementProcessed();
    void incrementProcessed(long count);
    void reportEntitySpan(Span span);

}
