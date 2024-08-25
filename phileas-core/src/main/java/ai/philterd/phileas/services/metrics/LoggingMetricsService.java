package ai.philterd.phileas.services.metrics;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.Filter;
import ai.philterd.phileas.model.services.MetricsService;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class LoggingMetricsService implements MetricsService {

    private static final Logger LOGGER = LogManager.getLogger(LoggingMetricsService.class);

    long processed = 0;
    private final Map<FilterType, Long> filterTypes = new HashMap<>();
    private final Map<FilterType, DescriptiveStatistics> filterTimes = new HashMap<>();

    @Override
    public void incrementProcessed() {
        incrementProcessed(1);
    }

    @Override
    public void incrementProcessed(long count) {
        processed += count;
        LOGGER.debug("Documents processed: {}", processed);
    }

    @Override
    public void incrementFilterType(FilterType filterType) {
        if(filterTypes.containsKey(filterType)) {
            filterTypes.put(filterType, filterTypes.get(filterType) + 1);
        } else {
            filterTypes.put(filterType, 1L);
        }
        LOGGER.debug("Filter type: {}, Count: {}", filterType, filterTypes.get(filterType));
    }

    @Override
    public void logFilterTime(FilterType filterType, long timeMs) {
        if(filterTimes.containsKey(filterType)) {
            filterTimes.get(filterType).addValue(timeMs);
        } else {
            final DescriptiveStatistics stats = new DescriptiveStatistics();
            stats.addValue(timeMs);
            filterTimes.put(filterType, stats);
        }
        LOGGER.debug("Filter type: {}, Average Time: {} ms", filterType, filterTimes.get(filterType).getMean());
    }

}
