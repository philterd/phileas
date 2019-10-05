package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class DateFilter extends RegexFilter implements Serializable {

    public static final Pattern DATE_YYYYMMDD_REGEX = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}");
    public static final Pattern DATE_MMDDYYYY_REGEX = Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}");
    public static final Pattern DATE_MDYYYY_REGEX = Pattern.compile("\\b\\d{1,2}-\\d{1,2}-\\d{2,4}");
    public static final Pattern DATE_MONTH_REGEX = Pattern.compile("(?i)(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)\\D?(\\d{1,2}(\\D?(st|nd|rd|th))?\\D?)?(\\D?((19[7-9]\\d|20\\d{2})|\\d{2}))?", Pattern.CASE_INSENSITIVE);

    public DateFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService) {
        super(FilterType.DATE, strategies, anonymizationService);
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        final List<Span> spans = new LinkedList<>();
        spans.addAll(findSpans(filterProfile, DATE_YYYYMMDD_REGEX, input, context, documentId));
        spans.addAll(findSpans(filterProfile, DATE_MMDDYYYY_REGEX, input, context, documentId));
        spans.addAll(findSpans(filterProfile, DATE_MDYYYY_REGEX, input, context, documentId));
        spans.addAll(findSpans(filterProfile, DATE_MONTH_REGEX, input, context, documentId));

        return spans;

    }

}
