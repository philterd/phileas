package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.enums.FilterType;

import java.util.concurrent.TimeUnit;

public interface MetricsService {

    void incrementProcessed();
    void incrementProcessed(long count);
    void incrementFilterType(FilterType filterType);

    /**
     * The elapsed time for each filter.
     * @param filterType A {@link FilterType filterType}.
     * @param timeMs The filter execution time in milliseconds.
     */
    void logFilterTime(FilterType filterType, long timeMs);

}
