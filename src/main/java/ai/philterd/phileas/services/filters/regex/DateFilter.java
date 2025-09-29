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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.regex.RegexFilter;
import ai.philterd.phileas.model.objects.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.services.SpanValidator;

import java.util.*;
import java.util.regex.Pattern;

public class DateFilter extends RegexFilter {

    private final SpanValidator spanValidator;
    private final boolean onlyValidDates;

    final private List<String> delimiters = Arrays.asList("-", "/", " ");

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
    public FilterResult filter(Policy policy, String contextName, Map<String, String> context, String documentId, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = new LinkedList<>();

        final List<Span> rawSpans = findSpans(policy, analyzer, input, contextName, context, documentId, attributes);

        if(onlyValidDates) {

            for(final Span span : rawSpans) {

                if(span.isAlwaysValid() || spanValidator.validate(span)) {

                    // The date is valid.
                    LOGGER.debug("Date {} for pattern {} is valid.", span.getText(), span.getPattern());
                    spans.add(span);

                } else {

                    // The date is invalid.
                    LOGGER.debug("Date {} for pattern {} is invalid.", span.getText(), span.getPattern());

                }

            }

        } else {

            // We are not worried about invalid dates formatted properly, e.g. 12/35/2019.
            spans.addAll(rawSpans);

        }

        return new FilterResult(contextName, documentId, Span.dropOverlappingSpans(spans));

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
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [\\d]{2}\\b"), 0.75).withFormat("MMMM yy").withAlwaysValid(true).build());
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [\\d]{4}\\b"), 0.75).withFormat("MMMM yyyy").withAlwaysValid(true).build());

        // The spans below DO have replaceable delimiters.

        for(final String delimiter : delimiters) {

            // DateTimeFormatter patterns: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
            // 'u' is year. 'y' is year-of-era.

            // Make a filter pattern for each pattern with each delimiter.
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}" + delimiter + "\\d{2}" + delimiter + "\\d{2}"), 0.75).withFormat("uuuu-MM-dd".replaceAll("-", delimiter)).build());
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}" + delimiter + "\\d{2}" + delimiter + "\\d{4}"), 0.75).withFormat("MM-dd-uuuu".replaceAll("-", delimiter)).build());
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{1,2}" + delimiter + "\\d{1,2}" + delimiter + "\\d{2,4}"), 75).withFormat("M-d-u".replaceAll("-", delimiter)).build());
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{1,2}" + delimiter + "\\d{2,4}"), 75).withFormat("M-u".replaceAll("-", delimiter)).build());
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)(\\.)?\\D?(\\d{1,2}(\\D?(st|nd|rd|th))?\\D?)(\\D?((19[7-9]\\d|20\\d{2})|\\d{2}))?\\b"), 0.75).withFormat("MMMM-dd".replaceAll("-", delimiter)).withAlwaysValid(true).build());

        }

        return filterPatterns;

    }

}
