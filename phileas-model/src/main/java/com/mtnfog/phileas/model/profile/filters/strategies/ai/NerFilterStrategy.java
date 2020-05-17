package com.mtnfog.phileas.model.profile.filters.strategies.ai;

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

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class NerFilterStrategy extends AbstractFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(NerFilterStrategy.class);

    private static FilterType filterType = FilterType.NER_ENTITY;

    @Override
    public boolean evaluateCondition(String context, String documentId, String token, String condition, Map<String, Object> attributes) {

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        boolean conditionsSatisfied = false;

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(StringUtils.equalsIgnoreCase(TOKEN, parsedCondition.getField())) {

                conditionsSatisfied = evaluateTokenCondition(parsedCondition, token);

            } else if(StringUtils.equalsIgnoreCase(TYPE, parsedCondition.getField())) {

                final String entityType = attributes.getOrDefault(TYPE, "unk").toString();

                if(parsedCondition.getOperator().equalsIgnoreCase("==")) {
                    conditionsSatisfied = StringUtils.equalsIgnoreCase(entityType, parsedCondition.getValue());
                } else if(parsedCondition.getOperator().equalsIgnoreCase("!=")) {
                    conditionsSatisfied = !StringUtils.equalsIgnoreCase(entityType, parsedCondition.getValue());
                } else {
                    // Invalid operator.
                    LOGGER.warn("Invalid comparator on NER filter strategy condition: {}", condition);
                }

                break;

            } else if(StringUtils.equalsIgnoreCase(CONFIDENCE, parsedCondition.getField())) {

                final double confidence = (double) attributes.getOrDefault(CONFIDENCE, 0.00);
                final double threshold = Double.valueOf(parsedCondition.getValue());

                switch (parsedCondition.getOperator()) {
                    case ">":
                        conditionsSatisfied = (confidence > threshold);
                        break;
                    case "<":
                        conditionsSatisfied = (confidence < threshold);
                        break;
                    case ">=":
                        conditionsSatisfied = (confidence >= threshold);
                        break;
                    case "<=":
                        conditionsSatisfied = (confidence <= threshold);
                        break;
                    case "==":
                        conditionsSatisfied = (confidence == threshold);
                        break;
                    case "!=":
                        conditionsSatisfied = (confidence != threshold);
                        break;

                }

            } else if(StringUtils.equalsIgnoreCase(CONTEXT, parsedCondition.getField())) {

                final String conditionContext = parsedCondition.getValue();

                switch (parsedCondition.getOperator()) {
                    case "==":
                        conditionsSatisfied = (StringUtils.equalsIgnoreCase("\"" + context + "\"", conditionContext));
                        break;
                    case "!=":
                        conditionsSatisfied = !(StringUtils.equalsIgnoreCase("\"" + context + "\"", conditionContext));
                        break;

                }

            }

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
