package io.philterd.phileas.services.filters.regex;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.filter.FilterConfiguration;
import io.philterd.phileas.model.filter.rules.regex.RegexFilter;
import io.philterd.phileas.model.objects.Analyzer;
import io.philterd.phileas.model.objects.FilterPattern;
import io.philterd.phileas.model.objects.FilterResult;
import io.philterd.phileas.model.objects.Span;
import io.philterd.phileas.model.profile.FilterProfile;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class PhoneNumberExtensionFilter extends RegexFilter {

    public PhoneNumberExtensionFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.PHONE_NUMBER_EXTENSION, filterConfiguration);

        final Pattern phoneNumberExtendionPattern = Pattern.compile("\\bx[0-9]+\\b");
        final FilterPattern phoneExtension1 = new FilterPattern.FilterPatternBuilder(phoneNumberExtendionPattern, 0.75).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("phone");
        this.contextualTerms.add("extension");
        this.contextualTerms.add("ext");

        this.analyzer = new Analyzer(contextualTerms, phoneExtension1);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}
