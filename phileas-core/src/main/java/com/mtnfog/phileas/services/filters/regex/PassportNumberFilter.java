package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

public class PassportNumberFilter extends RegexFilter implements Serializable {

    private static final Map<String, Pattern> PASSPORT_NUMBERS = new HashMap<>();

    public PassportNumberFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.PASSPORT_NUMBER, strategies, anonymizationService, ignored, crypto, windowSize);

        // https://www.e-verify.gov/about-e-verify/e-verify-data/e-verify-enhancements/june-2011
        // U.S. Passport numbers must be between six and nine alphanumeric characters (letters and numbers).
        // The "C" that precedes a U.S. Passport Card number is no longer case sensitive.
        // U.S. visa numbers must be exactly eight alphanumeric characters (letters and numbers). Entering a visa number is still optional though if an employee provides one, we encourage you to enter it, as doing so may prevent a tentative nonconfirmation (TNC).

        // https://passportinfo.com/blog/what-is-my-passport-number/
        
        PASSPORT_NUMBERS.put("US", Pattern.compile("\\b([A-Za-z0-9][6-9])\\b"));

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        for(final String country : PASSPORT_NUMBERS.keySet()) {

            final Pattern pattern = PASSPORT_NUMBERS.get(country);

            // TODO: How to set the country code?
            spans.addAll(findSpans(filterProfile, pattern, input, context, documentId));

        }

        return spans;

    }

}