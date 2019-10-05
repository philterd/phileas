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

public class IpAddressFilter extends RegexFilter implements Serializable {

    private static final Pattern IPV4_PATTERN = Pattern.compile("([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])");
    private static final Pattern IPV6_PATTERN = Pattern.compile("(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}", Pattern.CASE_INSENSITIVE);

    // TODO: What is this here for?
    private static final Pattern IPV6_HEX_PATTERN = Pattern.compile("((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)", Pattern.CASE_INSENSITIVE);

    public IpAddressFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService) {
        super(FilterType.IP_ADDRESS, strategies, anonymizationService);
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        final List<Span> spans = new LinkedList<>();
        spans.addAll(findSpans(filterProfile, IPV4_PATTERN, input, context, documentId));
        spans.addAll(findSpans(filterProfile, IPV6_PATTERN, input, context, documentId));

        return spans;

    }

}
