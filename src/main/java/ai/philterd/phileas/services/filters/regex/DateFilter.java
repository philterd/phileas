/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
                    // format is correct. The strategies read the date from its text in the same order.
                    LOGGER.debug("Date for pattern {} is valid as day-first.", span.getPattern());
                    span.setPattern(toDayFirstFormat(span.getPattern()));
                    spans.add(span);

                } else {

                    // The date is invalid.
                    LOGGER.debug("Date for pattern {} is invalid.", span.getPattern());

                }

            }

        } else {

            // We are not worried about invalid dates formatted properly, e.g. 12/35/2019. A date that is
            // only valid day first still records the day-first format, as with onlyValidDates.
            for(final Span span : rawSpans) {
                if(!span.isAlwaysValid() && spanValidator != null && !spanValidator.validate(span) && validatesAsDayFirst(span)) {
                    span.setPattern(toDayFirstFormat(span.getPattern()));
                }
            }

            spans.addAll(rawSpans);

        }

        return new Filtered(context, Span.dropOverlappingSpans(spans));

    }

    /**
     * Build the list of date filter patterns.
     * @return A list of {@link FilterPattern}.
     */
    // Per-delimiter pattern construction (Pattern.compile() inside the loop, the "\d{2}"/"\d{4}"
    // literals repeated across the four numeric templates) predates this fix; hoisting it into
    // delimiter-independent constants would be a separate, larger restructuring of this method.
    @SuppressWarnings({"java:S9142", "java:S1192"})
    private List<FilterPattern> buildFilterPatterns() {

        final List<FilterPattern> filterPatterns = new LinkedList<>();

        // Day-first month-name dates, such as "27 March, 1965", "3rd Mar 2020", or "27-Mar-1965". The
        // month-first patterns below also match some of these through their optional leading day, but
        // carry a month-first format, and none of them allows a comma between the month and the year.
        // These patterns come first so that, for the same text, the span carries the day-first format.
        // See issue #462.
        final String dayFirst = "(?i)\\b(?:0?[1-9]|[12]\\d|3[01])(?:st|nd|rd|th)?";
        // The month-name alternation carries most of the regex complexity of these patterns, as it does
        // in the month-first patterns below; matching month names more loosely and checking them in Java
        // would be a separate change.
        @SuppressWarnings("java:S5843")
        final String monthName = "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember|t)?|Oct(?:ober)?|(?:Nov|Dec)(?:ember)?)\\.?";
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile(dayFirst + " " + monthName + ", \\d{4}\\b"), 0.75).withFormat("dd MMMM, yyyy").withAlwaysValid(true).build());
        for(final String delimiter : delimiters) {
            final String d = Pattern.quote(delimiter);
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile(dayFirst + d + monthName + d + "\\d{4}\\b"), 0.75).withFormat("dd-MMMM-yyyy".replace("-", delimiter)).withAlwaysValid(true).build());
        }

        // These spans do NOT have replaceable delimiters.
        // These dates with the month names are pretty specific so they always pass validation as valid dates.
        // A day may have one or two digits, and "MMMM yy" may not take a day followed by a four-digit
        // year as its year, as in "March 27, 1965" or "March 27 - 1965".
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember|t)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [\\d]{1,2}, [\\d]{4}\\b"), 0.75).withFormat("MMMM dd, yyyy").withAlwaysValid(true).build());
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember|t)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [\\d]{1,2} [\\d]{4}\\b"), 0.75).withFormat("MMMM dd yyyy").withAlwaysValid(true).build());

        // What may separate a month-first date's day from its year: a comma, one or two hyphens, or an
        // en or em dash, with up to two spaces on either side, or one to three spaces.
        final String yearSeparator = "(?: {0,2}(?:,|-{1,2}|[\\u2013\\u2014]) {0,2}| {1,3})";
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember|t)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [\\d]{2}\\b(?!(?:\\D{1,2}|" + yearSeparator + ")\\d{4}\\b)"), 0.75).withFormat("MMMM yy").withAlwaysValid(true).build());
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember|t)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [\\d]{4}\\b"), 0.75).withFormat("MMMM yyyy").withAlwaysValid(true).build());

        // The spans below DO have replaceable delimiters.

        for(final String delimiter : delimiters) {

            // DateTimeFormatter patterns: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
            // 'u' is year. 'y' is year-of-era.

            // The delimiter is inserted literally into the date regex, so quote it for the regex (a
            // "." must match a literal dot, not any character) while keeping the raw delimiter for the
            // human-readable date format.
            final String d = Pattern.quote(delimiter);

            // Any complete numeric date. The month-and-year shape is left out for "." for the same
            // reason it is not generated there: it would make a decimal such as 3.14 a date.
            final String anyNumericDate = "(?:\\d{4}" + d + "\\d{2}" + d + "\\d{2}"
                    + "|\\d{1,2}" + d + "\\d{1,2}" + d + "\\d{2,4}"
                    + (".".equals(delimiter) ? "" : "|\\d{1,2}" + d + "\\d{2,4}") + ")";

            // A match may not stop partway through a longer number ("25" out of "255") or a longer
            // delimited chain ("1-20" out of "1-20-01023"). A continuation is allowed only when all
            // that is left decomposes into dates, so "2024-06-01-2024-06-30" keeps both. Unbounded,
            // the repetition overflows the stack on a long chain. Space is exempt: it separates
            // ordinary words too. See issues #341 and #353.
            final String noMoreDigits = " ".equals(delimiter)
                    ? "(?!\\d)"
                    : "(?!\\d)(?:(?!" + d + "\\d)|(?=(?:" + d + anyNumericDate + "){1,8}(?!\\d)(?!" + d + "\\d)))";

            // Make a filter pattern for each pattern with each delimiter. The numeric day/month/year
            // patterns carry a month-first format; a date that month-first cannot validate (e.g.
            // 25/12/1980) records its day-first format in filter(). See toDayFirstFormat. The date
            // strategies read the date from its text, not from this format.
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}" + d + "\\d{2}" + d + "\\d{2}" + noMoreDigits), 0.75).withFormat("uuuu-MM-dd".replace("-", delimiter)).build());
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}" + d + "\\d{2}" + d + "\\d{4}" + noMoreDigits), 0.75).withFormat("MM-dd-uuuu".replace("-", delimiter)).build());
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{1,2}" + d + "\\d{1,2}" + d + "\\d{2,4}" + noMoreDigits), 75).withFormat("M-d-u".replace("-", delimiter)).build());

            // Month-and-year only. Not generated for the "." delimiter, where it would match decimals
            // such as 3.14.
            if(!".".equals(delimiter)) {
                filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{1,2}" + d + "\\d{2,4}" + noMoreDigits), 75).withFormat("M-u".replace("-", delimiter)).build());
            }

            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember|t)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)(\\.)?\\D?(\\d{1,2}(\\D?(st|nd|rd|th))?\\D?)(\\D?(\\d{4}|\\d{2}))?\\b"), 0.75).withFormat("MMMM-dd".replaceAll("-", delimiter)).withAlwaysValid(true).build());

        }

        // A year set further from the day, as in "March 27 - 1965". Last, so the patterns above keep the
        // spans they match. A number set this far apart is often a quantity ("Mar 3 - 2000 mg"), so it is
        // taken as the year only from 1800 to 2100 and when no unit follows it. "g" and "L" are matched in
        // that case only, so an initial such as "G." is not read as grams. See issue #468.
        final String plausibleYear = "(?:1[89]\\d{2}|20\\d{2}|2100)";
        final String unit = "(?!\\s*(?:(?:mg|mcg|(?-i:g)|kg|ml|(?-i:L)|iu|mmol|units|points|items|boxes|pieces|dollars|usd|people|patients)\\b|%))";
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)\\b" + monthName + " \\d{1,2}(?:st|nd|rd|th)?" + yearSeparator + plausibleYear + "\\b" + unit), 0.75).withFormat("MMMM dd, yyyy").withAlwaysValid(true).build());

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
