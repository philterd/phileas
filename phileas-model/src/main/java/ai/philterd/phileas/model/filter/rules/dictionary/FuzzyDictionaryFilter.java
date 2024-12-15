package ai.philterd.phileas.model.filter.rules.dictionary;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.Policy;

import java.io.Serializable;
import java.util.Map;

public class FuzzyDictionaryFilter extends DictionaryFilter implements Serializable {

    /**
     * Creates a new dictionary-based filter.
     *
     * @param filterType
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     */
    public FuzzyDictionaryFilter(final FilterType filterType, final FilterConfiguration filterConfiguration) {
        super(filterType, filterConfiguration);
    }

    @Override
    public FilterResult filter(Policy policy, String context, String documentId, int piece, String input, Map<String, String> attributes) throws Exception {
        return null;
    }

}

