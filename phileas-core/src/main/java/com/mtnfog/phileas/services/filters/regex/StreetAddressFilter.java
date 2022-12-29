package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class StreetAddressFilter extends RegexFilter {

    public StreetAddressFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.STREET_ADDRESS, filterConfiguration);

        final Pattern addressPattern = Pattern.compile("\\b\\d{1,6} +.{2,25}\\b(avenue|ave|court|ct|street|st|drive|dr|lane|ln|road|rd|blvd|plaza|parkway|pkwy)[.,]?(.{0,25} +\\b\\d{5}\\b)?", Pattern.CASE_INSENSITIVE);
        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(addressPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("address");
        this.contextualTerms.add("location");

        this.analyzer = new Analyzer(contextualTerms, filterPattern);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}
