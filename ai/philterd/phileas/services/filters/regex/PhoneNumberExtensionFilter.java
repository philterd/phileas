package ai.philterd.phileas.services.filters.regex;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.regex.RegexFilter;
import ai.philterd.phileas.model.objects.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.FilterProfile;

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
