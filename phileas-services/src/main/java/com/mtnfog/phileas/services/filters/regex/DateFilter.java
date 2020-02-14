package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
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

    public static final Pattern DATE_YYYYMMDD_REGEX = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");
    public static final Pattern DATE_MMDDYYYY_REGEX = Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}\\b");
    public static final Pattern DATE_MDYYYY_REGEX = Pattern.compile("\\b\\d{1,2}-\\d{1,2}-\\d{2,4}\\b");
    public static final Pattern DATE_MONTH_REGEX = Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)\\D?(\\d{1,2}(\\D?(st|nd|rd|th))?\\D?)(\\D?((19[7-9]\\d|20\\d{2})|\\d{2}))?\\b", Pattern.CASE_INSENSITIVE);

    private Map<String, Pattern> datePatterns = new HashMap<>();

    private SpanValidator spanValidator;
    private boolean onlyValidDates;

    public DateFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, boolean onlyValidDates, SpanValidator spanValidator, Set<String> ignored, Crypto crypto) {
        super(FilterType.DATE, strategies, anonymizationService, ignored, crypto);

        this.spanValidator = spanValidator;
        this.onlyValidDates = onlyValidDates;

        final List<String> delimeters = Arrays.asList("-", "/", " ");

        for(final String delimeter : delimeters) {

            // DateTimeFormatter patterns: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
            // 'u' is year. 'y' is year-of-era.

            // Put an entry for each delimeter for each pattern.
            datePatterns.put("YYYY-MM-dd".replaceAll("-", delimeter), Pattern.compile("\\b\\d{4}" + delimeter + "\\d{2}" + delimeter + "\\d{2}"));
            datePatterns.put("MM-dd-YYYY".replaceAll("-", delimeter), Pattern.compile("\\b\\d{2}" + delimeter + "\\d{2}" + delimeter + "\\d{4}"));
            datePatterns.put("M-d-u".replaceAll("-", delimeter), Pattern.compile("\\b\\d{1,2}" + delimeter + "\\d{1,2}" + delimeter + "\\d{2,4}"));
            datePatterns.put("MMMM-dd".replaceAll("-", delimeter), DATE_MONTH_REGEX);

        }

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        for(String format : datePatterns.keySet()) {

            final List<Span> rawSpans = findSpans(filterProfile, datePatterns.get(format), input, context, documentId);

            // Set the date format for each span.
            rawSpans.forEach(s -> s.setPattern(format));

            if(onlyValidDates) {

                for(final Span span : rawSpans) {

                    if(spanValidator.validate(span)) {

                        // The date is valid.
                        LOGGER.info("Date {} for pattern {} is valid.", span.getText(), format);
                        spans.add(span);

                    } else {

                        // The date is invalid.
                        LOGGER.info("Date {} for pattern {} is invalid.", span.getText(), format);

                    }

                }

            } else {

                // We are not worried about invalid dates formatted properly, e.g. 12/35/2019.
                spans.addAll(rawSpans);

            }

        }

        return Span.dropOverlappingSpans(spans);

    }

}
