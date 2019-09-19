package com.mtnfog.phileas.model.filter.rules.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.RulesFilter;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;

/**
 * A filter that works by using one or more regular expressions.
 */
public abstract class RegexFilter extends RulesFilter implements Serializable {

    public RegexFilter(FilterType filterType, AnonymizationService anonymizationService) {
        super(filterType, anonymizationService);
    }

}
