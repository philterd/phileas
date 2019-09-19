package com.mtnfog.phileas.model.profile.filters.strategies.dynamic;

import com.mtnfog.phileas.model.conditions.ParsedCondition;
import com.mtnfog.phileas.model.conditions.ParserListener;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CityFilterStrategy extends AbstractFilterStrategy {

    private static FilterType filterType = FilterType.LOCATION_CITY;

    @Override
    public boolean evaluateCondition(String context, String documentId, String token, String condition, Map<String, Object> attributes) {

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        // TODO: PHI-134: Evaluate the condition.
        return true;
    }

    @Override
    public String getReplacement(String context, String documentId, String token, AnonymizationService anonymizationService) throws IOException {

        String replacement = null;

        if(StringUtils.equalsIgnoreCase(redactionFormat, REDACT)) {

            replacement = getValueOrDefault(redactionFormat, DEFAULT_REDACTION).replace("%t", filterType.getType());

        } else if(StringUtils.equalsIgnoreCase(redactionFormat, RANDOM_REPLACE)) {

            // Default to document scope.
            String scope = REPLACEMENT_SCOPE_DOCUMENT;

            if (StringUtils.equalsIgnoreCase(replacementScope, REPLACEMENT_SCOPE_CONTEXT)) {
                scope = REPLACEMENT_SCOPE_CONTEXT;
            }

            replacement = getAnonymizedToken(scope, token, anonymizationService);

        } else if(StringUtils.equalsIgnoreCase(redactionFormat, STATIC_REPLACE)) {

            replacement = staticReplacement;

        } else {

            // Default to redaction.
            replacement = getValueOrDefault(redactionFormat, DEFAULT_REDACTION).replace("%t", filterType.getType());

        }

        return replacement;

    }

}
