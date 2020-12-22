package com.mtnfog.phileas.model.profile.filters.strategies.ai;

import com.mtnfog.phileas.model.conditions.ParsedCondition;
import com.mtnfog.phileas.model.conditions.ParserListener;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.model.utils.Encryption;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class NerFilterStrategy extends AbstractFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(NerFilterStrategy.class);

    private static FilterType filterType = FilterType.NER_ENTITY;

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public boolean evaluateCondition(String context, String documentId, String token, String condition, double confidence, String classification) {

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        boolean conditionsSatisfied = false;

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(StringUtils.equalsIgnoreCase(TOKEN, parsedCondition.getField())) {

                conditionsSatisfied = evaluateTokenCondition(parsedCondition, token);

            } else if(StringUtils.equalsIgnoreCase(CLASSIFICATION, parsedCondition.getField()) || StringUtils.equalsIgnoreCase(TYPE, parsedCondition.getField())) {

                final String entityType = classification;

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

                final double threshold = Double.parseDouble(parsedCondition.getValue());

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

            }

            // Short-circuit if we have a failure.
            if(!conditionsSatisfied) break;

        }

        return conditionsSatisfied;

    }

    @Override
    public Replacement getReplacement(String label, String context, String documentId, String token, String[] window, Crypto crypto, AnonymizationService anonymizationService, FilterPattern filterPattern) throws Exception {

        String replacement = null;
        String salt = "";

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

            if (isSalt()) {
                salt = RandomStringUtils.randomAlphanumeric(16);
            }

            replacement = DigestUtils.sha256Hex(token + salt);

        } else if(StringUtils.equalsIgnoreCase(strategy, ABBREVIATE)) {

            // TODO: Make PER a constant somewhere.
            // Philter-NER is only returning PER entities at this point.
            if(StringUtils.equalsIgnoreCase(label, "PER")) {
                replacement = WordUtils.initials(token, null);
            }

        } else {

            // Default to redaction.
            replacement = getRedactedToken(token, label, filterType);

        }

        return new Replacement(replacement, salt);

    }

}
