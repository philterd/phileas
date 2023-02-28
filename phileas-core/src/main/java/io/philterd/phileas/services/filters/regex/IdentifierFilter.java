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

public class IdentifierFilter extends RegexFilter {

    public IdentifierFilter(FilterConfiguration filterConfiguration, String classification, String regex, boolean caseSensitive) {
        super(FilterType.IDENTIFIER, filterConfiguration);

        final Pattern pattern;

        if(caseSensitive) {
            pattern = Pattern.compile(regex);
        } else {
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }

        // TODO: Expose initialConfidence via the filter profile.
        // TODO: Expose the contextual terms via the filter profile.
        final FilterPattern id1 = new FilterPattern.FilterPatternBuilder(pattern, 0.90).withClassification(classification).build();

        // There are no contextual terms because we don't know what they would be.
        // TODO: Let the user set a list of contextual terms?
        this.contextualTerms = new HashSet<>();
        this.analyzer = new Analyzer(contextualTerms, id1);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}
