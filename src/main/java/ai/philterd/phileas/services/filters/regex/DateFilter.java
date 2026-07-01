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
package ai.philterd.phileas.services.filters.regex;

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.regex.RegexFilter;
import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.Analyzer;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.validators.SpanValidator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateFilter extends RegexFilter {

    private final SpanValidator spanValidator;
    private final boolean onlyValidDates;

    // A month-first numeric date format (e.g. M/d/u, MM-dd-uuuu, MM.dd.uuuu) whose month and day
    // components can be swapped to its day-first equivalent. The two delimiters must be identical.
    private static final Pattern MONTH_FIRST_NUMERIC = Pattern.compile("^(M{1,2})(\\W)(d{1,2})\\2(u{1,4})$");

    final private List<String> delimiters = Arrays.asList("-", "/", " ", ".");

    public DateFilter(FilterConfiguration filterConfiguration, boolean onlyValidDates, SpanValidator spanValidator) {
        super(FilterType.DATE, filterConfiguration);

        this.spanValidator = spanValidator;
        this.onlyValidDates = onlyValidDates;

        final List<FilterPattern> filterPatterns = buildFilterPatterns();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("date");
        this.contextualTerms.add("day");
        this.contextualTerms.add("birthdate");
        this.contextualTerms.add("dob");
        this.contextualTerms.add("d.o.b.");

        this.analyzer = new Analyzer(contextualTerms, filterPatterns);

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        final List<Span> rawSpans = findSpans(contextService, policy, analyzer, input, context);

        if(onlyValidDates) {

            for(final Span span : rawSpans) {

                if(span.isAlwaysValid() || spanValidator.validate(span)) {

                    // The date is valid.
                    LOGGER.debug("Date for pattern {} is valid.", span.getPattern());
                    spans.add(span);

                } else if(validatesAsDayFirst(span)) {

                    // The month-first interpretation was not a real date, but the same text is a
                    // valid day-first date (e.g. 25/12/1980), so keep it: the date is redacted rather
                    // than left in the clear. Record the day-first format on the span so its reported
                    // format is correct. Note the replacement was already computed upstream with the
                    // month-first format, so a SHIFT/TRUNCATE strategy falls back to redaction for a
                    // day-first date (it is removed, but not transformed).
                    LOGGER.debug("Date for pattern {} is valid as day-first.", span.getPattern());
                    span.setPattern(toDayFirstFormat(span.getPattern()));
                    spans.add(span);

                } else {

                    // The date is invalid.
                    LOGGER.debug("Date for pattern {} is invalid.", span.getPattern());

                }

            }

        } else {

            // We are not worried about invalid dates formatted properly, e.g. 12/35/2019.
            spans.addAll(rawSpans);

        }

        return new Filtered(context, Span.dropOverlappingSpans(spans));

    }

    /**
     * Build the list of date filter patterns.
     * @return A list of {@link FilterPattern}.
     */
    private List<FilterPattern> buildFilterPatterns() {

        final List<FilterPattern> filterPatterns = new LinkedList<>();

        // These spans do NOT have replaceable delimiters.
        // These dates with the month names are pretty specific so they always pass validation as valid dates.
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [\\d]{0,1}, [\\d]{4}\\b"), 0.75).withFormat("MMMM dd, yyyy").withAlwaysValid(true).build());
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [\\d]{0,1} [\\d]{4}\\b"), 0.75).withFormat("MMMM dd yyyy").withAlwaysValid(true).build());
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [\\d]{2}\\b"), 0.75).withFormat("MMMM yy").withAlwaysValid(true).build());
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [\\d]{4}\\b"), 0.75).withFormat("MMMM yyyy").withAlwaysValid(true).build());

        // The spans below DO have replaceable delimiters.

        for(final String delimiter : delimiters) {

            // DateTimeFormatter patterns: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
            // 'u' is year. 'y' is year-of-era.

            // The delimiter is inserted literally into the date regex, so quote it for the regex (a
            // "." must match a literal dot, not any character) while keeping the raw delimiter for the
            // human-readable date format.
            final String d = Pattern.quote(delimiter);

            // Make a filter pattern for each pattern with each delimiter. The numeric day/month/year
            // patterns carry a month-first format; day-first dates (e.g. 25/12/1980) that month-first
            // cannot validate are recovered in the filter() validation step, which retries the same
            // text as day-first. See toDayFirstFormat / validate().
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}" + d + "\\d{2}" + d + "\\d{2}"), 0.75).withFormat("uuuu-MM-dd".replaceAll("-", delimiter)).build());
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}" + d + "\\d{2}" + d + "\\d{4}"), 0.75).withFormat("MM-dd-uuuu".replaceAll("-", delimiter)).build());
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{1,2}" + d + "\\d{1,2}" + d + "\\d{2,4}"), 75).withFormat("M-d-u".replaceAll("-", delimiter)).build());

            // Month-and-year only. Not generated for the "." delimiter, where it would match decimals
            // such as 3.14.
            if(!".".equals(delimiter)) {
                filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{1,2}" + d + "\\d{2,4}"), 75).withFormat("M-u".replaceAll("-", delimiter)).build());
            }

            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)(\\.)?\\D?(\\d{1,2}(\\D?(st|nd|rd|th))?\\D?)(\\D?((19[7-9]\\d|20\\d{2})|\\d{2}))?\\b"), 0.75).withFormat("MMMM-dd".replaceAll("-", delimiter)).withAlwaysValid(true).build());

        }

        return filterPatterns;

    }

    /**
     * Validates the span's text against the day-first equivalent of its (month-first) format. The
     * span is not modified.
     * @param span The {@link Span} to validate.
     * @return {@code true} if the span has a day-first-convertible format and its text is a valid
     * date in day-first order.
     */
    private boolean validatesAsDayFirst(final Span span) {

        final String dayFirstFormat = toDayFirstFormat(span.getPattern());

        if(dayFirstFormat == null) {
            return false;
        }

        final String originalFormat = span.getPattern();

        try {
            span.setPattern(dayFirstFormat);
            return spanValidator.validate(span);
        } finally {
            span.setPattern(originalFormat);
        }

    }

    /**
     * Converts a month-first numeric date format to its day-first equivalent by swapping the month
     * and day components, e.g. {@code M/d/u} to {@code d/M/u} and {@code MM-dd-uuuu} to
     * {@code dd-MM-uuuu}. Returns {@code null} for any format that is not a month-first numeric
     * day/month/year format (year-first, month-and-year, and month-name formats are left alone).
     * @param format The date format.
     * @return The day-first format, or {@code null}.
     */
    private static String toDayFirstFormat(final String format) {

        if(format == null) {
            return null;
        }

        final Matcher matcher = MONTH_FIRST_NUMERIC.matcher(format);

        if(matcher.matches()) {
            // groups: 1=month, 2=delimiter, 3=day, 4=year. Swap month and day.
            return matcher.group(3) + matcher.group(2) + matcher.group(1) + matcher.group(2) + matcher.group(4);
        }

        return null;

    }

}
