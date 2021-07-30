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

public class CurrencyFilter extends RegexFilter {

    public CurrencyFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.CURRENCY, filterConfiguration);
        
        // See https://stackoverflow.com/a/14174261/1428388
        final Pattern currencyPattern1 = Pattern.compile("\\b\\ ?[+-]?[0-9]{1,3}(?:,?[0-9])*(?:\\.[0-9]{1,2})?\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern currency1 = new FilterPattern.FilterPatternBuilder(currencyPattern1, 0.80).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("dollars");
        this.contextualTerms.add("amount");

        this.analyzer = new Analyzer(contextualTerms, currency1);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        return new FilterResult(context, documentId, nonOverlappingSpans);

    }

}
