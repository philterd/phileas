package io.philterd.phileas.services.filters.regex;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.filter.FilterConfiguration;
import io.philterd.phileas.model.filter.rules.regex.RegexFilter;
import io.philterd.phileas.model.objects.Analyzer;
import io.philterd.phileas.model.objects.FilterPattern;
import io.philterd.phileas.model.objects.FilterResult;
import io.philterd.phileas.model.objects.Span;
import io.philterd.phileas.model.profile.Crypto;
import io.philterd.phileas.model.profile.FilterProfile;
import io.philterd.phileas.model.profile.IgnoredPattern;
import io.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import io.philterd.phileas.model.services.AlertService;
import io.philterd.phileas.model.services.AnonymizationService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MacAddressFilter extends RegexFilter {

    public MacAddressFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.MAC_ADDRESS, filterConfiguration);

        final Pattern macAddressPattern = Pattern.compile("\\b([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})\\b");
        final FilterPattern macAddress1 = new FilterPattern.FilterPatternBuilder(macAddressPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("mac");
        this.contextualTerms.add("network");

        this.analyzer = new Analyzer(contextualTerms, macAddress1);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}
