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

public class SectionFilter extends RegexFilter {

    public SectionFilter(FilterConfiguration filterConfiguration, String startPattern, String endPattern) {
        super(FilterType.SECTION, filterConfiguration);

        final Pattern pattern = Pattern.compile(startPattern + "(.*?)" + endPattern);
        final FilterPattern sectionPattern1 = new FilterPattern.FilterPatternBuilder(pattern, 0.90).build();

        // There are no contextual terms because it doesn't make sense to have them for a section.
        this.contextualTerms = new HashSet<>();

        this.analyzer = new Analyzer(sectionPattern1);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}
