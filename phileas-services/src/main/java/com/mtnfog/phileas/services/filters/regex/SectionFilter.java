package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class SectionFilter extends RegexFilter implements Serializable {

    private Pattern regex;

    public SectionFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, String startPattern, String endPattern, Set<String> ignored, Crypto crypto) {
        super(FilterType.SECTION, strategies, anonymizationService, ignored, crypto);

        regex = Pattern.compile(startPattern + "(.*?)" + endPattern);
System.out.println(regex.toString());
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        return findSpans(filterProfile, regex, input, context, documentId);

    }

}
