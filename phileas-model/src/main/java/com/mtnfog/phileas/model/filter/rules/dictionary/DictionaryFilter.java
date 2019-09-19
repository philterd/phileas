package com.mtnfog.phileas.model.filter.rules.dictionary;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.RulesFilter;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;

/**
 * A filter that operates on a preset list of dictionary words.
 */
public abstract class DictionaryFilter extends RulesFilter implements Serializable {

    public DictionaryFilter(FilterType filterType, AnonymizationService anonymizationService) {

        super(filterType, anonymizationService);

    }

}
