package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;

public class PhoneNumberExtensionFilter extends RegexFilter implements Serializable {

    private static final Pattern EXTENSION_REGEX = Pattern.compile("\\bx[0-9]+\\b");

    public PhoneNumberExtensionFilter(AnonymizationService anonymizationService) {
        super(FilterType.PHONE_NUMBER_EXTENSION, anonymizationService);
    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws IOException {

        return findSpans(filterProfile, EXTENSION_REGEX, input, context, documentId);

    }

}
