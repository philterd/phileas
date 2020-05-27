package com.mtnfog.phileas.model.filter.rules.dictionary;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.RulesFilter;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * A filter that operates on a preset list of dictionary words.
 */
public abstract class DictionaryFilter extends RulesFilter {

    public DictionaryFilter(FilterType filterType, List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Crypto crypto, int windowSize) {

        super(filterType, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

    }

}
