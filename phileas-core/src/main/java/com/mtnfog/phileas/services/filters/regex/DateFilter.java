package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.phileas.model.services.SpanValidator;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

public class DateFilter extends RegexFilter implements Serializable {

    private SpanValidator spanValidator;
    private boolean onlyValidDates;

    public DateFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, boolean onlyValidDates, SpanValidator spanValidator, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.DATE, strategies, anonymizationService, ignored, crypto, windowSize);

        this.spanValidator = spanValidator;
        this.onlyValidDates = onlyValidDates;

        final List<FilterPattern> filterPatterns = new LinkedList<>();
        final List<String> delimiters = Arrays.asList("-", "/", " ");

        for(final String delimiter : delimiters) {

            // DateTimeFormatter patterns: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
            // 'u' is year. 'y' is year-of-era.

            // Make a filter pattern for each pattern with each delimiter.
            filterPatterns.add(new FilterPattern(Pattern.compile("\\b\\d{4}" + delimiter + "\\d{2}" + delimiter + "\\d{2}"), "YYYY-MM-dd".replaceAll("-", delimiter), 0.75));
            filterPatterns.add(new FilterPattern(Pattern.compile("\\b\\d{2}" + delimiter + "\\d{2}" + delimiter + "\\d{4}"), "MM-dd-YYYY".replaceAll("-", delimiter), 0.75));
            filterPatterns.add(new FilterPattern(Pattern.compile("\\b\\d{1,2}" + delimiter + "\\d{1,2}" + delimiter + "\\d{2,4}"), "M-d-u".replaceAll("-", delimiter), 0.75));
            filterPatterns.add(new FilterPattern(Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)\\D?(\\d{1,2}(\\D?(st|nd|rd|th))?\\D?)(\\D?((19[7-9]\\d|20\\d{2})|\\d{2}))?\\b"), "MMMM-dd".replaceAll("-", delimiter), 0.75));

        }

        this.contextualTerms = new HashSet<>(){{
            add("date");
            add("day");
        }};

        this.analyzer = new Analyzer(contextualTerms, filterPatterns);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        final List<Span> rawSpans = findSpans(filterProfile, analyzer, input, context, documentId);

        if(onlyValidDates) {

            for(final Span span : rawSpans) {

                if(spanValidator.validate(span)) {

                    // The date is valid.
                    LOGGER.info("Date {} for pattern {} is valid.", span.getText(), span.getPattern());
                    spans.add(span);

                } else {

                    // The date is invalid.
                    LOGGER.info("Date {} for pattern {} is invalid.", span.getText(), span.getPattern());

                }

            }

        } else {

            // We are not worried about invalid dates formatted properly, e.g. 12/35/2019.
            spans.addAll(rawSpans);

        }

        return Span.dropOverlappingSpans(spans);

    }

}
