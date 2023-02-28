package io.philterd.phileas.model.filter.rules.regex;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.filter.FilterConfiguration;
import io.philterd.phileas.model.filter.rules.RulesFilter;
import io.philterd.phileas.model.objects.Analyzer;
import io.philterd.phileas.model.objects.DocumentAnalysis;

/**
 * A filter that works by using one or more regular expressions.
 */
public abstract class RegexFilter extends RulesFilter {

    protected Analyzer analyzer;

    /**
     * Creates a new regular expression-based filter.
     * @param filterType
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     */
    public RegexFilter(FilterType filterType, FilterConfiguration filterConfiguration) {
        super(filterType, filterConfiguration);
    }

}
