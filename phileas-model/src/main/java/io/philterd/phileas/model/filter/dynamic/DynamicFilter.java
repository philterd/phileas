package io.philterd.phileas.model.filter.dynamic;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.filter.Filter;
import io.philterd.phileas.model.filter.FilterConfiguration;

public abstract class DynamicFilter extends Filter {

    /**
     * Creates a new dynamic filter.
     * @param filterType
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     */
    public DynamicFilter(FilterType filterType, FilterConfiguration filterConfiguration) {
        super(filterType, filterConfiguration);
    }

}
