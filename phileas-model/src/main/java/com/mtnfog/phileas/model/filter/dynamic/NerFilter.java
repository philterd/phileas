package com.mtnfog.phileas.model.filter.dynamic;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.model.services.MetricsService;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.Serializable;
import java.util.Map;

/**
 * A dynamic filter that operates using named-entity recognition.
 */
public abstract class NerFilter extends DynamicFilter implements Serializable {

    protected static final int STATISTICS_WINDOW_SIZE = 10000;
    protected static final int SUFFICIENT_VALUES_COUNT = 50;

    protected Map<String, DescriptiveStatistics> stats;
    protected String type;

    protected MetricsService metricsService;

    /**
     * Creates a new filter.
     *
     * @param filterType The {@link FilterType type} of the filter.
     * @param stats A map of {@link DescriptiveStatistics}.
     * @param metricsService The {@link MetricsService}.
     * @param anonymizationService The {@link AnonymizationService} for this filter.
     */
    public NerFilter(FilterType filterType,
                     Map<String, DescriptiveStatistics> stats,
                     MetricsService metricsService,
                     AnonymizationService anonymizationService) {

        super(filterType, anonymizationService);

        this.stats = stats;
        this.metricsService = metricsService;

    }

}
