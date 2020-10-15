package com.mtnfog.phileas.model.filter.rules.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.RulesFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.IgnoredPattern;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * A filter that works by using one or more regular expressions.
 */
public abstract class RegexFilter extends RulesFilter {

    protected Analyzer analyzer;

    public RegexFilter(FilterType filterType, List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Set<String> ignoredFiles, List<IgnoredPattern> ignoredPatterns, Crypto crypto, int windowSize) {

        super(filterType, strategies, anonymizationService, alertService, ignored, ignoredFiles, ignoredPatterns, crypto, windowSize);

    }

}
