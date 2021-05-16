package com.mtnfog.phileas.model.filter.dynamic;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.objects.DocumentAnalysis;
import com.mtnfog.phileas.model.services.MetricsService;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Map;

/**
 * A dynamic filter that operates using named-entity recognition.
 */
public abstract class NerFilter extends DynamicFilter {

    private final Map<String, DescriptiveStatistics> stats;

    protected String type;
    protected MetricsService metricsService;
    protected boolean removePunctuation;
    protected Map<String, Double> thresholds;

    /**
     * Creates a new filter.
     *
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     * @param stats A map of {@link DescriptiveStatistics}.
     * @param metricsService The {@link MetricsService}.
     * @param removePunctuation
     * @param thresholds
     */
    public NerFilter(FilterConfiguration filterConfiguration,
                     Map<String, DescriptiveStatistics> stats,
                     MetricsService metricsService,
                     boolean removePunctuation,
                     Map<String, Double> thresholds) {

        super(FilterType.NER_ENTITY, filterConfiguration);

        this.stats = stats;
        this.metricsService = metricsService;
        this.removePunctuation = removePunctuation;
        this.thresholds = thresholds;

    }

}
