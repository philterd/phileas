package com.mtnfog.phileas.model.profile.filters.strategies.rules;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.conditions.ParsedCondition;
import com.mtnfog.phileas.model.conditions.ParserListener;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.metadata.zipcode.ZipCodeMetadataRequest;
import com.mtnfog.phileas.model.metadata.zipcode.ZipCodeMetadataResponse;
import com.mtnfog.phileas.model.metadata.zipcode.ZipCodeMetadataService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    @SerializedName("truncateDigits")
    @Expose
    private Integer truncateDigits;

    @Override
    public boolean evaluateCondition(String context, String documentId, String token, String condition, Map<String, Object> attributes) {

        boolean conditionsSatisfied = false;

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(StringUtils.equalsIgnoreCase(POPULATION, parsedCondition.getField())) {

                final int value = Integer.parseInt(parsedCondition.getValue());

                final ZipCodeMetadataResponse response = zipCodeMetadataService.getMetadata(new ZipCodeMetadataRequest(token));
                final long populationForZipCode = response.getPopulation();

                if (StringUtils.equalsIgnoreCase(POPULATION, parsedCondition.getField())) {

                    switch (parsedCondition.getOperator()) {
                        case ">":
                            conditionsSatisfied = (populationForZipCode > value);
                            break;
                        case "<":
                            conditionsSatisfied = (populationForZipCode < value);
                            break;
                        case ">=":
                            conditionsSatisfied = (populationForZipCode >= value);
                            break;
                        case "<=":
                            conditionsSatisfied = (populationForZipCode <= value);
                            break;
                        case "==":
                            conditionsSatisfied = (populationForZipCode == value);
                            break;
                        case "!=":
                            conditionsSatisfied = (populationForZipCode != value);
                            break;

                    }

                }

            } else if(StringUtils.equalsIgnoreCase(TOKEN, parsedCondition.getField())) {

                conditionsSatisfied = evaluateTokenCondition(parsedCondition, token);

            }

            LOGGER.debug("Condition for [" + condition + "] satisfied: " + conditionsSatisfied);

            // Short-circuit if we have a failure.
            if(!conditionsSatisfied) break;

        }

        return conditionsSatisfied;

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

        } else if(StringUtils.equalsIgnoreCase(redactionFormat, TRUNCATE)) {

            final int truncateLength = getValueOrDefault(truncateDigits, 2);
            replacement = token.substring(0, truncateDigits) + StringUtils.repeat("*", Math.min(token.length() - truncateLength, 5 - truncateDigits));

        } else if(StringUtils.equalsIgnoreCase(redactionFormat, ZERO_LEADING)) {

            replacement = "000" + token.subSequence(3, 5);

        } else {

            // Default to redaction.
            replacement = getValueOrDefault(redactionFormat, DEFAULT_REDACTION).replace("%t", filterType.getType());

        }

        return replacement;

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