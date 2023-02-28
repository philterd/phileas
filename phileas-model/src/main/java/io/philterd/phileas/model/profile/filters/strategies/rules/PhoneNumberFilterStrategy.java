package io.philterd.phileas.model.profile.filters.strategies.rules;

import io.philterd.phileas.model.conditions.ParsedCondition;
import io.philterd.phileas.model.conditions.ParserListener;
import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.objects.FilterPattern;
import io.philterd.phileas.model.objects.Replacement;
import io.philterd.phileas.model.profile.Crypto;
import io.philterd.phileas.model.profile.FPE;
import io.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import io.philterd.phileas.model.services.AnonymizationService;
import io.philterd.phileas.model.utils.Encryption;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class PhoneNumberFilterStrategy extends AbstractFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(PhoneNumberFilterStrategy.class);

    private static FilterType filterType = FilterType.PHONE_NUMBER;

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public boolean evaluateCondition(String context, String documentId, String token, String[] window, String condition, double confidence, String classification) {

        boolean conditionsSatisfied = false;

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(StringUtils.equalsIgnoreCase(TOKEN, parsedCondition.getField())) {

                conditionsSatisfied = evaluateTokenCondition(parsedCondition, token, window);

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

            }

            LOGGER.debug("Condition for [{}] satisfied: {}", condition, conditionsSatisfied);

            // Short-circuit if we have a failure.
            if(!conditionsSatisfied) break;

        }

        return conditionsSatisfied;

    }

    @Override
    public Replacement getReplacement(String label, String context, String documentId, String token, String[] window, Crypto crypto, FPE fpe, AnonymizationService anonymizationService, FilterPattern filterPattern) throws Exception {

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

        } else if(StringUtils.equalsIgnoreCase(strategy, FPE_ENCRYPT_REPLACE)) {

            replacement = Encryption.formatPreservingEncrypt(fpe, token);

        } else if(StringUtils.equalsIgnoreCase(strategy, HASH_SHA256_REPLACE)) {

            if(isSalt()) {
                salt = RandomStringUtils.randomAlphanumeric(16);
            }

            replacement = DigestUtils.sha256Hex(token + salt);

        } else {

            // Default to redaction.
            replacement = getRedactedToken(token, label, filterType);

        }

        return new Replacement(replacement, salt);

    }

}
