package com.mtnfog.phileas.services.filters.regex;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.ParseLocation;
import com.joestelmach.natty.Parser;
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

    private Map<String, Pattern> datePatterns = new HashMap<>();
    private List<String> delimeters = Arrays.asList("-", "/", ".", " ");

    private boolean onlyValidDates;

    public DateFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, boolean onlyValidDates, Set<String> ignored) {
        super(FilterType.DATE, strategies, anonymizationService, ignored);

        this.onlyValidDates = onlyValidDates;

        for(final String delimeter : delimeters) {

            // Put an entry for each delimeter for each pattern.
            datePatterns.put("YYYY-MM-dd".replaceAll("-", delimeter), Pattern.compile("\\b\\d{4}" + delimeter + "\\d{2}" + delimeter + "\\d{2}"));
            datePatterns.put("MM-dd-YYYY".replaceAll("-", delimeter), Pattern.compile("\\b\\d{2}" + delimeter + "\\d{2}" + delimeter + "\\d{4}"));
            datePatterns.put("M-d-YY".replaceAll("-", delimeter), Pattern.compile("\\b\\d{1,2}" + delimeter + "\\d{1,2}" + delimeter + "\\d{2,4}"));
            datePatterns.put("MMMM-dd".replaceAll("-", delimeter), DATE_MONTH_REGEX);

        }

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        final List<Span> spans = new LinkedList<>();

        /*Parser parser = new Parser();
        List<DateGroup> groups = parser.parse(input);
        for(DateGroup group : groups) {

            final List<Date> dates = group.getDates();
            final int line = group.getLine();
            final int column = group.getPosition();
            final String matchingValue = group.getText();
            final String syntaxTree = group.getSyntaxTree().toStringTree();
            final  Map<String, List<ParseLocation>> parseMap = group.getParseLocations();
            //boolean isRecurreing = group.isRecurring();
            //Date recursUntil = group.getRecursUntil();

            //public static Span make(int characterStart, int characterEnd, FilterType filterType, String context,
            //        String documentId, double confidence, String text, String replacement, boolean ignored) {

            Span span = Span.make(group.getPosition() - 1, group.getPosition() + group.getText().length() - 1,
                    FilterType.DATE, context, documentId, 1.0, group.getText(), "replacement", false);

            spans.add(span);

        }*/

        for(String format : datePatterns.keySet()) {

            //LOGGER.info("Finding dates with pattern {}", datePatterns.get(format));

            // TODO: I think I need to save the pattern that found the span so it can later
            // be used to validate the date is an actual date.
            // OR, I could add a new "Validator" interface and pass an implementation into findSpans()
            // so the date (or other object) is validated as soon as it is found.
            final List<Span> rawSpans = findSpans(filterProfile, datePatterns.get(format), input, context, documentId);

            if(onlyValidDates) {

                for(final Span span : rawSpans) {

                    final SimpleDateFormat sdf = new SimpleDateFormat(format);
                    sdf.setLenient(false);

                    if(isDateValid(span.getText(), format)) {
                    //if(DateValidator.getInstance().validate(dateWithoutDelimeters, format, Locale.US) == null) {

                        // The date is invalid.
                        LOGGER.info("Date {} for pattern {} is invalid.", span.getText(), format);

                    } else {

                        // The date is valid.
                        LOGGER.info("Date {} for pattern {} is valid.", span.getText(), format);
                        spans.add(span);


                    }
                }

            } else {

                // We are not worried about invalid dates formatted properly, e.g. 12/35/2019.
                spans.addAll(rawSpans);

            }

        }

        return Span.dropOverlappingSpans(spans);

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
