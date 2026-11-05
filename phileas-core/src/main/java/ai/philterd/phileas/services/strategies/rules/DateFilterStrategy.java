/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.strategies.rules;

import ai.philterd.phileas.model.conditions.ParsedCondition;
import ai.philterd.phileas.model.conditions.ParserListener;
import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.anonymization.AnonymizationService;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.utils.Encryption;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.time.Clock;
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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class DateFilterStrategy extends AbstractFilterStrategy {

    private static final Logger LOGGER = LogManager.getLogger(DateFilterStrategy.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final FilterType filterType = FilterType.DATE;

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

    private transient Clock clock = Clock.systemDefaultZone();

    // The date filter's numeric formats: month, day, and year letters with one delimiter, as in M-d-u.
    private static final Pattern NUMERIC_FORMAT = Pattern.compile("(?:M{1,2}|d{1,2}|u{1,4})([-/ .])(?:M{1,2}|d{1,2}|u{1,4})(?:\\1(?:M{1,2}|d{1,2}|u{1,4}))?");

    /**
     * The date filter adds the date-only strategies and implements neither FPE_ENCRYPT_REPLACE,
     * LAST_4 nor MAP_REPLACE.
     */
    @Override
    public Set<String> getAcceptedStrategies() {
        return Set.of(REDACT, RANDOM_REPLACE, STATIC_REPLACE, CRYPTO_REPLACE, HASH_SHA256_REPLACE, MASK,
                TRUNCATE, TRUNCATE_TO_YEAR, SHIFT, RELATIVE);
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public boolean evaluateCondition(Policy policy, String context, String token, String[] window, String condition, double confidence) {

        boolean conditionsSatisfied = false;

        final List<ParsedCondition> parsedConditions = ParserListener.getTerminals(condition);

        for(ParsedCondition parsedCondition : parsedConditions) {

            if(Strings.CI.equals(TOKEN, parsedCondition.getField())) {

                conditionsSatisfied = evaluateTokenCondition(parsedCondition, token, window);

            } else if(Strings.CI.equals(CONTEXT, parsedCondition.getField())) {

                final String conditionContext = parsedCondition.getValue();

                conditionsSatisfied = switch (parsedCondition.getOperator()) {
                    case EQUALS -> (Strings.CI.equals("\"" + context + "\"", conditionContext));
                    case NOT_EQUALS -> !(Strings.CI.equals("\"" + context + "\"", conditionContext));
                    default -> conditionsSatisfied;
                };

            } else if(Strings.CI.equals(CONFIDENCE, parsedCondition.getField())) {

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

            }

            // Short-circuit if we have a failure.
            if(!conditionsSatisfied) break;

        }

        return conditionsSatisfied;

    }

    @Override
    public Replacement getReplacement(ContextService contextService, String label, String context, String token, String[] window, Crypto crypto, FPE fpe, AnonymizationService anonymizationService, FilterPattern filterPattern) throws Exception {

        String replacement;
        String salt = "";

        if(Strings.CI.equals(strategy, REDACT)) {

            replacement = getRedactedToken(token, label, filterType);

        } else if(Strings.CI.equals(strategy, MASK)) {

            int characters = token.length();

            if(!Strings.CI.equals(maskLength, AbstractFilterStrategy.SAME)) {
                characters = Integer.parseInt(maskLength);
            }

            if(characters < 1) {
                characters = 5;
            }

            replacement = maskCharacter.repeat(characters);

        } else if(Strings.CI.equals(strategy, TRUNCATE)) {

            replacement = truncate(token);

        } else if(Strings.CI.equals(strategy, RANDOM_REPLACE)) {

            AnonymizationService as = anonymizationService;
            if (this.anonymizationService != null) {
                as = this.anonymizationService;
            }

            replacement = getAnonymizedToken(contextService, replacementScope, token, as, filterType.getType());

        } else if(Strings.CI.equals(strategy, STATIC_REPLACE)) {

            replacement = staticReplacement;

        } else if(Strings.CI.equals(strategy, CRYPTO_REPLACE)) {

            replacement = "{{" + Encryption.encrypt(token, crypto) + "}}";

        } else if(Strings.CI.equals(strategy, HASH_SHA256_REPLACE)) {

            if (isSalt()) {
                salt = RandomStringUtils.secure().nextAlphanumeric(16);
            }

            replacement = DigestUtils.sha256Hex(token + salt);

        } else if(Strings.CI.equals(strategy, TRUNCATE_TO_YEAR)) {

            // The year as written, so a two-digit year stays two digits.
            replacement = readsFromText(filterPattern)
                    ? read(token, filterPattern).map(DateText::yearText).orElseGet(() -> unparsed(token, label, filterPattern))
                    : truncateToYearWithFormat(token, label, filterPattern);

        } else if(Strings.CI.equals(strategy, SHIFT)) {

            // A month and year alone is redacted: shifting it by days alone would leave it unchanged.
            replacement = readsFromText(filterPattern)
                    ? read(token, filterPattern).filter(DateText::hasDay).map(date -> date.format(shift(date.date())))
                            .orElseGet(() -> unparsed(token, label, filterPattern))
                    : shiftWithFormat(token, label, filterPattern);

        } else if(Strings.CI.equals(strategy, RELATIVE)) {

            // Make the date relative such as "3 months ago." A month and year alone is taken as the first
            // of the month.
            replacement = readsFromText(filterPattern)
                    ? read(token, filterPattern).map(date -> getReadableDate(date.date().atStartOfDay(), LocalDateTime.now(clock()), token, label, filterType))
                            .orElseGet(() -> unparsed(token, label, filterPattern))
                    : relativeWithFormat(token, label, filterPattern);

        } else {

            // Default to redaction.
            replacement = getRedactedToken(token, label, filterType);

        }

        return new Replacement(replacement, salt);

    }

    /** Shifts a date by the strategy's days, months, and years, or by random amounts with shiftRandom. */
    private LocalDate shift(final LocalDate date) {

        if(shiftRandom) {
            final int randomShiftDays = RANDOM.nextInt(1, 30);
            final int randomShiftMonths = RANDOM.nextInt(1, 12);
            final int randomShiftYears = -RANDOM.nextInt(1, 3);
            return date.plusDays(randomShiftDays).plusMonths(randomShiftMonths).plusYears(randomShiftYears);
        }

        return date.plusDays(shiftDays).plusMonths(shiftMonths).plusYears(shiftYears);

    }

    /** The current year, for reading two-digit years. */
    private int currentYear() {
        return LocalDate.now(clock()).getYear();
    }

    // The field is transient, so a strategy created without its constructor has no clock.
    private Clock clock() {
        return clock == null ? Clock.systemDefaultZone() : clock;
    }

    /** Sets the clock the current date and year are read from. For tests. */
    void setClock(final Clock clock) {
        this.clock = clock;
    }

    /**
     * Whether the span was found by one of the date filter's patterns, whose text is read with
     * {@link MonthNameDate} or {@link NumericDate} rather than with the pattern's format. A date with any other
     * format, such as one a caller supplies, is read with its format.
     */
    private static boolean readsFromText(final FilterPattern filterPattern) {
        return isMonthName(filterPattern) || isNumeric(filterPattern);
    }

    /** A month-name pattern's format, such as {@code MMMM dd, yyyy} or {@code dd-MMMM-yyyy}. */
    private static boolean isMonthName(final FilterPattern filterPattern) {
        return filterPattern != null && filterPattern.getFormat() != null && filterPattern.getFormat().contains("MMMM");
    }

    /** A numeric pattern's format, such as {@code MM/dd/uuuu}, {@code M-d-u}, {@code uuuu.MM.dd}, or {@code M/u}. */
    private static boolean isNumeric(final FilterPattern filterPattern) {
        return filterPattern != null && filterPattern.getFormat() != null && NUMERIC_FORMAT.matcher(filterPattern.getFormat()).matches();
    }

    private Optional<? extends DateText> read(final String token, final FilterPattern filterPattern) {

        if(isMonthName(filterPattern)) {
            return MonthNameDate.parse(token, currentYear());
        }

        // The pattern's format says whether the month and day are zero-padded, as in MM/dd/uuuu.
        return NumericDate.parse(token, currentYear(), filterPattern.getFormat().contains("MM"));

    }

    private String truncateToYearWithFormat(final String token, final String label, final FilterPattern filterPattern) {

        try {

            final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(filterPattern.getFormat(), Locale.US).withResolverStyle(ResolverStyle.STRICT);
            return String.valueOf(LocalDate.parse(token, dtf).getYear());

        } catch (DateTimeParseException ex) {

            // The DateTimeParseException message echoes the unparsed date text, so it is not logged.
            LOGGER.error("Unable to parse date with format {}. Falling back to redaction.", filterPattern.getFormat());
            return getRedactedToken(token, label, filterType);

        }

    }

    private String shiftWithFormat(final String token, final String label, final FilterPattern filterPattern) {

        try {

            final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(filterPattern.getFormat(), Locale.US).withResolverStyle(ResolverStyle.STRICT);
            return shift(LocalDate.parse(token, dtf)).atStartOfDay().format(dtf);

        } catch (DateTimeParseException ex) {

            // The DateTimeParseException message echoes the unparsed date text, so it is not logged.
            LOGGER.error("Unable to parse date with format {}. Falling back to redaction.", filterPattern.getFormat());
            return getRedactedToken(token, label, filterType);

        }

    }

    private String relativeWithFormat(final String token, final String label, final FilterPattern filterPattern) {

        try {

            final DateTimeFormatter dtf = new DateTimeFormatterBuilder()
                    .appendPattern(filterPattern.getFormat())
                    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                    .toFormatter();

            return getReadableDate(LocalDate.parse(token, dtf).atStartOfDay(), LocalDateTime.now(clock()), token, label, filterType);

        } catch (DateTimeParseException ex) {

            // The DateTimeParseException message echoes the unparsed date text, so it is not logged.
            LOGGER.error("Unable to parse date. Falling back to redaction.");
            return getRedactedToken(token, label, filterType);

        }

    }

    /** Redacts a date the strategy cannot transform, such as an invalid date or a month name and one number. */
    private String unparsed(final String token, final String label, final FilterPattern filterPattern) {
        // The date's text is not logged.
        LOGGER.debug("Unable to transform a date with format {}. Falling back to redaction.",
                filterPattern == null ? null : filterPattern.getFormat());
        return getRedactedToken(token, label, filterType);
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
