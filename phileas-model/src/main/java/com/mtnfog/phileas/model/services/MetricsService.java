package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.enums.FilterType;

public interface MetricsService {

    void incrementProcessed();
    void incrementProcessed(long count);
    void incrementFilterType(FilterType filterType);

}
