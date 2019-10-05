package com.mtnfog.phileas.model.filter.rules.dictionary;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.RulesFilter;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;
import java.util.List;

/**
 * A filter that operates on a preset list of dictionary words.
 */
public abstract class DictionaryFilter extends RulesFilter implements Serializable {

    public DictionaryFilter(FilterType filterType, List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService) {

        super(filterType, strategies, anonymizationService);

    }

}
