/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.model.policy.filters.strategies.rules;

import ai.philterd.phileas.model.policy.Policy;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.conditions.ParsedCondition;
import ai.philterd.phileas.model.conditions.ParserListener;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.policy.Crypto;
import ai.philterd.phileas.model.policy.FPE;
import ai.philterd.phileas.model.policy.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.phileas.model.utils.Encryption;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DateFilterStrategy extends AbstractFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(DateFilterStrategy.class);

    private static final FilterType filterType = FilterType.DATE;

    @SerializedName("shiftRandom")
    @Expose
    private boolean shiftRandom = false;

    @SerializedName("shiftDays")
    @Expose
    private Integer shiftDays = 0;

    @SerializedName("shiftMonths")
    @Expose
    private Integer shiftMonths = 0;

    @SerializedName("shiftYears")
    @Expose
    private Integer shiftYears = 0;

    @SerializedName("futureDates")
    @Expose
    private boolean futureDates = false;

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public boolean evaluateCondition(Policy policy, String context, String documentId, String token, String[] window, String condition, double confidence, Map<String, String> attributes) {

        boolean conditionsSatisfied = false;

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(StringUtils.equalsIgnoreCase(TOKEN, parsedCondition.getField())) {

                conditionsSatisfied = evaluateTokenCondition(parsedCondition, token, window);

            } else if(StringUtils.equalsIgnoreCase(CONTEXT, parsedCondition.getField())) {

                final String conditionContext = parsedCondition.getValue();

                conditionsSatisfied = switch (parsedCondition.getOperator()) {
                    case EQUALS -> (StringUtils.equalsIgnoreCase("\"" + context + "\"", conditionContext));
                    case NOT_EQUALS -> !(StringUtils.equalsIgnoreCase("\"" + context + "\"", conditionContext));
                    default -> conditionsSatisfied;
                };

            } else if(StringUtils.equalsIgnoreCase(CONFIDENCE, parsedCondition.getField())) {

                final double threshold = Double.parseDouble(parsedCondition.getValue());

                conditionsSatisfied = switch (parsedCondition.getOperator()) {
                    case GREATER_THAN -> (confidence > threshold);
                    case LESS_THAN -> (confidence < threshold);
                    case GREATER_THAN_EQUALS -> (confidence >= threshold);
                    case LESS_THAN_EQUALS -> (confidence <= threshold);
                    case EQUALS -> (confidence == threshold);
                    case NOT_EQUALS -> (confidence != threshold);
                    default -> conditionsSatisfied;
                };

            } else if(StringUtils.equalsIgnoreCase(SENTIMENT, parsedCondition.getField())) {

                // If there is no sentiment attribute, the condition is automatically not satisfied.
                if(attributes.containsKey("sentiment")) {

                    final int documentSentiment = Integer.parseInt(attributes.get("sentiment"));
                    final int sentimentCondition = Integer.parseInt(parsedCondition.getValue());

                    conditionsSatisfied = switch (parsedCondition.getOperator()) {
                        case GREATER_THAN -> (documentSentiment > sentimentCondition);
                        case LESS_THAN -> (documentSentiment < sentimentCondition);
                        case GREATER_THAN_EQUALS -> (documentSentiment >= sentimentCondition);
                        case LESS_THAN_EQUALS -> (documentSentiment <= sentimentCondition);
                        case EQUALS -> (documentSentiment == sentimentCondition);
                        case NOT_EQUALS -> (documentSentiment != sentimentCondition);
                        default -> conditionsSatisfied;
                    };

                } else {
                    conditionsSatisfied = false;
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

        } else if(StringUtils.equalsIgnoreCase(strategy, MASK)) {

            int characters = token.length();

            if(!StringUtils.equalsIgnoreCase(maskLength, AbstractFilterStrategy.SAME)) {
                characters = Integer.parseInt(maskLength);
            }

            if(characters < 1) {
                characters = 5;
            }

            replacement = maskCharacter.repeat(characters);

        } else if(StringUtils.equalsIgnoreCase(strategy, TRUNCATE)) {

            int truncateLength = getValueOrDefault(truncateDigits, 4);

            if (truncateLength < 1) {
                truncateLength = 1;
            }

            if(StringUtils.equalsIgnoreCase(truncateDirection, LEADING)) {
                replacement = token.substring(0, truncateLength) + StringUtils.repeat(truncateCharacter, token.length() - truncateLength);
            } else {
                replacement = StringUtils.repeat(truncateCharacter, token.length() - truncateLength) + token.substring(token.length() - truncateLength);
            }

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

        } else if(StringUtils.equalsIgnoreCase(strategy, TRUNCATE_TO_YEAR)) {

            try {

                final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(filterPattern.getFormat(), Locale.US).withResolverStyle(ResolverStyle.STRICT);
                final LocalDateTime parsedDate = LocalDate.parse(token, dtf).atStartOfDay();

                replacement = String.valueOf(parsedDate.getYear());

            } catch (DateTimeParseException ex) {

                LOGGER.error("Unable to parse date with format " + filterPattern.getFormat() + ". Falling back to redaction.", ex);

                // This will be thrown if the input date is not a valid date.
                // Default back to redaction.
                replacement = getRedactedToken(token, label, filterType);

            }

        } else if(StringUtils.equalsIgnoreCase(strategy, SHIFT)) {

            // Shift the date given some number of days, months, and/or years.

            try {

                final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(filterPattern.getFormat(), Locale.US).withResolverStyle(ResolverStyle.STRICT);
                final LocalDateTime parsedDate = LocalDate.parse(token, dtf).atStartOfDay();

                // Shift the date. Only valid dates can be shifted.
                if(shiftRandom == true) {
                    // Shifting based on a random days, months, years.
                    final int randomShiftDays = RandomUtils.nextInt(1, 30);
                    final int randomShiftMonths = RandomUtils.nextInt(1, 12);
                    final int randomShiftYears = 0 - RandomUtils.nextInt(1, 3);
                    final LocalDateTime shiftedDate = parsedDate.plusDays(randomShiftDays).plusMonths(randomShiftMonths).plusYears(randomShiftYears);
                    replacement = shiftedDate.format(dtf);
                } else {
                    // Shifting based on the given days, months, and years given.
                    final LocalDateTime shiftedDate = parsedDate.plusDays(shiftDays).plusMonths(shiftMonths).plusYears(shiftYears);
                    replacement = shiftedDate.format(dtf);
                }

            } catch (DateTimeParseException ex) {

                LOGGER.error("Unable to parse date with format " + filterPattern.getFormat() + ". Falling back to redaction.", ex);

                // This will be thrown if the input date is not a valid date.
                // Default back to redaction.
                replacement = getRedactedToken(token, label, filterType);

            }

        } else if(StringUtils.equalsIgnoreCase(strategy, RELATIVE)) {

            // Make the date relative such as "3 months ago."

            try {

                final DateTimeFormatter dtf = new DateTimeFormatterBuilder()
                        .appendPattern(filterPattern.getFormat())
                        .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                        .toFormatter();

                final LocalDateTime parsedDate = LocalDate.parse(token, dtf).atStartOfDay();
                final LocalDateTime currentDate = LocalDateTime.now();

                // Convert the date to a spelled out date.
                replacement = getReadableDate(parsedDate, currentDate, token, label, filterType);

            } catch (DateTimeParseException ex) {

                LOGGER.error("Unable to parse date. Falling back to redaction.", ex);

                // This will be thrown if the input date is not a valid date.
                // Default back to redaction.
                replacement = getRedactedToken(token, label, filterType);

            }

        } else {

            // Default to redaction.
            replacement = getRedactedToken(token, label, filterType);

        }

        return new Replacement(replacement, salt);

    }

    public String getReadableDate(LocalDateTime parsedDate, LocalDateTime currentDate, String token, String label, FilterType filterType) {

        final String replacement;

        final Period period = Period.between(parsedDate.toLocalDate(), currentDate.toLocalDate());

        // Only convert past dates to relative.
        if(!period.isNegative()) {

            int months = Math.abs(period.getMonths());

            if (period.getDays() >= 15) {
                months = months + 1;
            }

            final int years = Math.abs(period.getYears());

            String relative = years + " years " + months + " months ago";

            if (years == 0) {
                relative = months + " months ago";
            }

            replacement = relative;

        } else {

            if(futureDates) {

                int months = Math.abs(period.getMonths());

                if (period.getDays() >= 15) {
                    months = months + 1;
                }

                final int years = Math.abs(period.getYears());

                String relative = "in " + years + " years " + months + " months";

                if (years == 0) {
                    relative = "in " + months + " months";
                }

                replacement = relative;

            } else {

                // Ignore future dates so just redact them.
                replacement = getRedactedToken(token, label, filterType);

            }

        }

        return replacement;

    }

    public Integer getShiftDays() {
        return shiftDays;
    }

    public void setShiftDays(Integer shiftDays) {
        this.shiftDays = shiftDays;
    }

    public Integer getShiftMonths() {
        return shiftMonths;
    }

    public void setShiftMonths(Integer shiftMonths) {
        this.shiftMonths = shiftMonths;
    }

    public Integer getShiftYears() {
        return shiftYears;
    }

    public void setShiftYears(Integer shiftYears) {
        this.shiftYears = shiftYears;
    }

    public boolean getFutureDates() {
        return futureDates;
    }

    public void setFutureDates(boolean futureDates) {
        this.futureDates = futureDates;
    }

    public void setShiftRandom(boolean shiftRandom) {
        this.shiftRandom = shiftRandom;
    }

    public boolean isShiftRandom() {
        return this.shiftRandom;
    }

}
