package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;

public interface MetricsService {

    void incrementProcessed();
    void incrementProcessed(long count);
    void reportEntitySpan(Span span);
    void incrementFilterType(FilterType filterType);

}
