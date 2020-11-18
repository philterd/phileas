package com.mtnfog.phileas.model.profile.filters.strategies.rules;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.conditions.ParsedCondition;
import com.mtnfog.phileas.model.conditions.ParserListener;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.metadata.zipcode.ZipCodeMetadataRequest;
import com.mtnfog.phileas.model.metadata.zipcode.ZipCodeMetadataResponse;
import com.mtnfog.phileas.model.metadata.zipcode.ZipCodeMetadataService;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.model.utils.Encryption;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class ZipCodeFilterStrategy extends AbstractFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(ZipCodeFilterStrategy.class);

    public static final String TRUNCATE = "truncate";
    public static final String POPULATION = "population";
    public static final String ZERO_LEADING = "zero_leading";

    private static FilterType filterType = FilterType.ZIP_CODE;

    private transient ZipCodeMetadataService zipCodeMetadataService;

    public ZipCodeFilterStrategy() throws IOException {
        this.zipCodeMetadataService = new ZipCodeMetadataService();
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    @SerializedName("truncateDigits")
    @Expose
    private Integer truncateDigits;

    @Override
    public boolean evaluateCondition(String context, String documentId, String token, String condition, double confidence, String classification) {

        boolean conditionsSatisfied = false;

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(StringUtils.equalsIgnoreCase(POPULATION, parsedCondition.getField())) {

                final int value = Integer.parseInt(parsedCondition.getValue());

                final ZipCodeMetadataResponse response = zipCodeMetadataService.getMetadata(new ZipCodeMetadataRequest(token));
                final long populationForZipCode = response.getPopulation();

                if (StringUtils.equalsIgnoreCase(POPULATION, parsedCondition.getField())) {

                    switch (parsedCondition.getOperator()) {
                        case GREATER_THAN:
                            conditionsSatisfied = (populationForZipCode > value);
                            break;
                        case LESS_THAN:
                            conditionsSatisfied = (populationForZipCode < value);
                            break;
                        case GREATER_THAN_EQUALS:
                            conditionsSatisfied = (populationForZipCode >= value);
                            break;
                        case LESS_THAN_EQUALS:
                            conditionsSatisfied = (populationForZipCode <= value);
                            break;
                        case EQUALS:
                            conditionsSatisfied = (populationForZipCode == value);
                            break;
                        case NOT_EQUALS:
                            conditionsSatisfied = (populationForZipCode != value);
                            break;

                    }

                }

            } else if(StringUtils.equalsIgnoreCase(TOKEN, parsedCondition.getField())) {

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
    public Replacement getReplacement(String label, String context, String documentId, String token, Crypto crypto, AnonymizationService anonymizationService, FilterPattern filterPattern) throws Exception {

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

        } else if(StringUtils.equalsIgnoreCase(strategy, TRUNCATE)) {

            final int truncateLength = getValueOrDefault(truncateDigits, 2);
            replacement = token.substring(0, truncateDigits) + StringUtils.repeat("*", Math.min(token.length() - truncateLength, 5 - truncateDigits));

        } else if(StringUtils.equalsIgnoreCase(strategy, ZERO_LEADING)) {

            replacement = "000" + token.subSequence(3, 5);

        } else if(StringUtils.equalsIgnoreCase(strategy, CRYPTO_REPLACE)) {

            replacement = "{{" + Encryption.encrypt(token, crypto) + "}}";

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

    public Integer getTruncateDigits() {
        return truncateDigits;
    }

    public void setTruncateDigits(Integer truncateDigits) {

        // Make sure it is a valid value.
        if(truncateDigits >= 1 && truncateDigits <= 4) {
            this.truncateDigits = truncateDigits;
        } else {
            throw new IllegalArgumentException("Truncate length must be between 1 and 4, inclusive.");
        }

    }

}