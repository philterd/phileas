package ai.philterd.phileas.services.filters.regex;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.regex.RegexFilter;
import ai.philterd.phileas.model.objects.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.Crypto;
import ai.philterd.phileas.model.profile.FilterProfile;
import ai.philterd.phileas.model.profile.IgnoredPattern;
import ai.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.model.services.AnonymizationService;

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
