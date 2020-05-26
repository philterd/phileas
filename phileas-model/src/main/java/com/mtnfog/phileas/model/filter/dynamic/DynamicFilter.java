package com.mtnfog.phileas.model.filter.dynamic;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public abstract class DynamicFilter extends Filter implements Serializable {

    /**
     * Creates a new dynamic filter.
     * @param filterType The {@link FilterType type} of the filter.
     * @param anonymizationService The {@link AnonymizationService} for this filter.
     */
    public DynamicFilter(FilterType filterType, List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(filterType, strategies, anonymizationService, alertService, ignored, crypto, windowSize);
    }

}
