package com.mtnfog.phileas.model.profile.filters.strategies.custom;

import com.mtnfog.phileas.model.conditions.ParsedCondition;
import com.mtnfog.phileas.model.conditions.ParserListener;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CustomDictionaryFilterStrategy extends AbstractFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(com.mtnfog.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy.class);

    private static FilterType filterType = FilterType.CUSTOM_DICTIONARY;

    @Override
    public boolean evaluateCondition(String context, String documentId, String token, String condition, Map<String, Object> attributes) {

        boolean conditionsSatisfied = false;

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(StringUtils.equalsIgnoreCase(TOKEN, parsedCondition.getField())) {

                conditionsSatisfied = evaluateTokenCondition(parsedCondition, token);

            }

            LOGGER.debug("Condition for [" + condition + "] satisfied: " + conditionsSatisfied);

            // Short-circuit if we have a failure.
            if(!conditionsSatisfied) break;

        }

        return conditionsSatisfied;

    }

    @Override
    public String getReplacement(String name, String context, String documentId, String token, AnonymizationService anonymizationService) throws IOException {

        String replacement = null;

        // Custom dictionary filter replacement does not support random replacement
        // because we don't know what kind of random value to generate.

        if(StringUtils.equalsIgnoreCase(redactionFormat, REDACT)) {

            replacement = getRedactedToken(name, filterType);

        } else if(StringUtils.equalsIgnoreCase(redactionFormat, STATIC_REPLACE)) {

            replacement = staticReplacement;

        } else {

            // Default to redaction.
            replacement = getRedactedToken(name, filterType);

        }

        return replacement;

    }

}
