package com.mtnfog.phileas.model.profile.filters.strategies.dynamic;

import com.mtnfog.phileas.model.conditions.ParsedCondition;
import com.mtnfog.phileas.model.conditions.ParserListener;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.model.utils.Encryption;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class StateFilterStrategy extends AbstractFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(StateFilterStrategy.class);

    private static FilterType filterType = FilterType.LOCATION_STATE;

    @Override
    public boolean evaluateCondition(String context, String documentId, String token, String condition, double confidence, String classification) {

        boolean conditionsSatisfied = false;

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(StringUtils.equalsIgnoreCase(TOKEN, parsedCondition.getField())) {

                conditionsSatisfied = evaluateTokenCondition(parsedCondition, token);

            } else if(StringUtils.equalsIgnoreCase(CONTEXT, parsedCondition.getField())) {

                final String conditionContext = parsedCondition.getValue();

                switch (parsedCondition.getOperator()) {
                    case EQUALS:
                        conditionsSatisfied = (StringUtils.equalsIgnoreCase("\"" + context + "\"", conditionContext));
                        break;
                    case NOT_EQUALS:
                        conditionsSatisfied = !(StringUtils.equalsIgnoreCase("\"" + context + "\"", conditionContext));
                        break;

                }

            } else if(StringUtils.equalsIgnoreCase(CONFIDENCE, parsedCondition.getField())) {

                final double threshold = Double.valueOf(parsedCondition.getValue());

                switch (parsedCondition.getOperator()) {
                    case GREATER_THAN:
                        conditionsSatisfied = (confidence > threshold);
                        break;
                    case LESS_THAN:
                        conditionsSatisfied = (confidence < threshold);
                        break;
                    case GREATER_THAN_EQUALS:
                        conditionsSatisfied = (confidence >= threshold);
                        break;
                    case LESS_THAN_EQUALS:
                        conditionsSatisfied = (confidence <= threshold);
                        break;
                    case EQUALS:
                        conditionsSatisfied = (confidence == threshold);
                        break;
                    case NOT_EQUALS:
                        conditionsSatisfied = (confidence != threshold);
                        break;

                }

            }

            LOGGER.debug("Condition for [{}] satisfied: {}", condition, conditionsSatisfied);

            // Short-circuit if we have a failure.
            if(!conditionsSatisfied) break;

        }

        return conditionsSatisfied;

    }

    @Override
    public String getReplacement(String label, String context, String documentId, String token, Crypto crypto, AnonymizationService anonymizationService) throws Exception {

        String replacement = null;

        if(StringUtils.equalsIgnoreCase(strategy, REDACT)) {

            replacement = getRedactedToken(token, label, filterType);

        } else if(StringUtils.equalsIgnoreCase(strategy, RANDOM_REPLACE)) {

            // Default to document scope.
            String scope = REPLACEMENT_SCOPE_DOCUMENT;

            if (StringUtils.equalsIgnoreCase(replacementScope, REPLACEMENT_SCOPE_CONTEXT)) {
                scope = REPLACEMENT_SCOPE_CONTEXT;
            }

            replacement = getAnonymizedToken(scope, token, anonymizationService);

        } else if(StringUtils.equalsIgnoreCase(strategy, STATIC_REPLACE)) {

            replacement = staticReplacement;

        } else if(StringUtils.equalsIgnoreCase(strategy, CRYPTO_REPLACE)) {

            replacement = "{{" + Encryption.encrypt(token, crypto) + "}}";

        } else if(StringUtils.equalsIgnoreCase(strategy, HASH_SHA256_REPLACE)) {

            replacement = DigestUtils.sha256Hex(token);

        } else {

            // Default to redaction.
            replacement = getRedactedToken(token, label, filterType);

        }

        return replacement;

    }

}
