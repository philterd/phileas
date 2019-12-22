package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.commons.validator.routines.DateValidator;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class DateFilter extends RegexFilter implements Serializable {

    public static final Pattern DATE_YYYYMMDD_REGEX = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}");
    public static final Pattern DATE_MMDDYYYY_REGEX = Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}");
    public static final Pattern DATE_MDYYYY_REGEX = Pattern.compile("\\b\\d{1,2}-\\d{1,2}-\\d{2,4}");
    public static final Pattern DATE_MONTH_REGEX = Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)\\D?(\\d{1,2}(\\D?(st|nd|rd|th))?\\D?)?(\\D?((19[7-9]\\d|20\\d{2})|\\d{2}))?", Pattern.CASE_INSENSITIVE);

    public Map<String, Pattern> datePatterns;

    private boolean onlyValidDates;

    public DateFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, boolean onlyValidDates, Set<String> ignored) {
        super(FilterType.DATE, strategies, anonymizationService, ignored);

        this.onlyValidDates = onlyValidDates;
        this.datePatterns = new HashMap<>();

        datePatterns.put("YYYY-MM-dd", DATE_YYYYMMDD_REGEX);
        datePatterns.put("MM-dd-YYYY", DATE_MMDDYYYY_REGEX);
        datePatterns.put("M-d-YY", DATE_MDYYYY_REGEX);
        datePatterns.put("MMMM dd", DATE_MONTH_REGEX);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        final List<Span> spans = new LinkedList<>();

        for(String format : datePatterns.keySet()) {

            final List<Span> rawSpans = findSpans(filterProfile, datePatterns.get(format), input, context, documentId);

            if(onlyValidDates) {

                for(Span span : rawSpans) {

                    // Because a date can use one of many delimeters, replace them all with a single delimeter.
                    // Refer to for the inspiration: https://stackoverflow.com/a/56888203/1428388
                    final String dateWithoutDelimeters = span.getText().replace("/", "-").replace(" ", "-");

                    final SimpleDateFormat sdf = new SimpleDateFormat(format);
                    sdf.setLenient(false);

                    if(isDateValid(span.getText(), format)) {
                    //if(DateValidator.getInstance().validate(dateWithoutDelimeters, format, Locale.US) == null) {

                        // The date is invalid.
                        LOGGER.info("Date {} ({}) for pattern {} is invalid.", span.getText(), dateWithoutDelimeters, format);

                    } else {

                        // The date is valid.
                        LOGGER.info("Date {} ({}) for pattern {} is valid.", span.getText(), dateWithoutDelimeters, format);
                        spans.add(span);


                    }
                }

            } else {

                // We are not worried about invalid dates formatted properly, e.g. 12/35/2019.
                spans.addAll(rawSpans);

            }

        }

        return spans;

    }

    public boolean isDateValid(String dateString, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            if (sdf.format(sdf.parse(dateString)).equals(dateString))
                return true;
        }
        catch (ParseException pe) {}

        return false;
    }

}
