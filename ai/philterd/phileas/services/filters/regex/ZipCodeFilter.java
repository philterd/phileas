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

public class ZipCodeFilter extends RegexFilter {

    public ZipCodeFilter(FilterConfiguration filterConfiguration, boolean requireDelimiter) {
        super(FilterType.ZIP_CODE, filterConfiguration);

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("zip");
        this.contextualTerms.add("zipcode");
        this.contextualTerms.add("postal");

        if(requireDelimiter) {

            // With delimeter
            final Pattern zipCodePattern = Pattern.compile("\\b[0-9]{5}(?:-[0-9]{4})?\\b");
            final FilterPattern zipCode = new FilterPattern.FilterPatternBuilder(zipCodePattern, 0.90).build();

            this.analyzer = new Analyzer(contextualTerms, zipCode);

        } else {

            // Without delimeter.
            final Pattern zipCodePattern = Pattern.compile("\\b[0-9]{5}(?:-?[0-9]{4})?\\b");
            final FilterPattern zipCode = new FilterPattern.FilterPatternBuilder(zipCodePattern, 0.50).build();

            this.analyzer = new Analyzer(contextualTerms, zipCode);

        }

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}
