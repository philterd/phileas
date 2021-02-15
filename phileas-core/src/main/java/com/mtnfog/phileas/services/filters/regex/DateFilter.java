package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.IgnoredPattern;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.model.services.SpanValidator;

import java.util.*;
import java.util.regex.Pattern;

public class DateFilter extends RegexFilter {

    private SpanValidator spanValidator;
    private boolean onlyValidDates;

    final private List<String> delimiters = Arrays.asList("-", "/", " ");

    public DateFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, boolean onlyValidDates, SpanValidator spanValidator, Set<String> ignored, Set<String> ignoredFiles, List<IgnoredPattern> ignoredPatterns, Crypto crypto, int windowSize) {
        super(FilterType.DATE, strategies, anonymizationService, alertService, ignored, ignoredFiles, ignoredPatterns, crypto, windowSize);

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
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        final List<Span> rawSpans = findSpans(filterProfile, analyzer, input, context, documentId);

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

        return new FilterResult(context, documentId, Span.dropOverlappingSpans(spans));

    }

    /**
     * Build the list of date filter patterns.
     * @return A list of {@link FilterPattern}.
     */
    private List<FilterPattern> buildFilterPatterns() {

        final List<FilterPattern> filterPatterns = new LinkedList<>();

        // These spans don't have replaceable delimeters.
        // These dates with the month names are pretty specific so they always pass validation as valid dates.
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [0-9]{0,1}, [0-9]{4}\\b"), 0.75).withFormat("MMMM dd, yyyy").withAlwaysValid(true).build());
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [0-9]{2}\\b"), 0.75).withFormat("MMMM yy").withAlwaysValid(true).build());
        filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?) [0-9]{4}\\b"), 0.75).withFormat("MMMM yyyy").withAlwaysValid(true).build());

        for(final String delimiter : delimiters) {

            // DateTimeFormatter patterns: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
            // 'u' is year. 'y' is year-of-era.

            // Make a filter pattern for each pattern with each delimiter.
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}" + delimiter + "\\d{2}" + delimiter + "\\d{2}"), 0.75).withFormat("uuuu-MM-dd".replaceAll("-", delimiter)).build());
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}" + delimiter + "\\d{2}" + delimiter + "\\d{4}"), 0.75).withFormat("MM-dd-uuuu".replaceAll("-", delimiter)).build());
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{1,2}" + delimiter + "\\d{1,2}" + delimiter + "\\d{2,4}"), 75).withFormat("M-d-u".replaceAll("-", delimiter)).build());
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{1,2}" + delimiter + "\\d{2,4}"), 75).withFormat("M-u".replaceAll("-", delimiter)).build());
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)\\D?(\\d{1,2}(\\D?(st|nd|rd|th))?\\D?)(\\D?((19[7-9]\\d|20\\d{2})|\\d{2}))?\\b"), 0.75).withFormat("MMMM-dd".replaceAll("-", delimiter)).build());

        }

        return filterPatterns;

    }

}
