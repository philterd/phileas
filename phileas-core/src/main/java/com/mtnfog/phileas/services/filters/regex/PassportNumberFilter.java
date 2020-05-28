package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class PassportNumberFilter extends RegexFilter implements Serializable {

    public PassportNumberFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.PASSPORT_NUMBER, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

        // https://www.e-verify.gov/about-e-verify/e-verify-data/e-verify-enhancements/june-2011
        // U.S. Passport numbers must be between six and nine alphanumeric characters (letters and numbers).
        // The "C" that precedes a U.S. Passport Card number is no longer case sensitive.
        // U.S. visa numbers must be exactly eight alphanumeric characters (letters and numbers). Entering a visa number is still optional though if an employee provides one, we encourage you to enter it, as doing so may prevent a tentative nonconfirmation (TNC).
        
        // U.S. Passports Issued: 1981-Current
        // TODO: Set the regex such that the initial two digits are valid and not just 0-9.
        // See the chart at https://passportinfo.com/blog/what-is-my-passport-number/.

        final FilterPattern passportUS = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b([0-9]{2}[A-Z0-9]{4,7})\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("passport");

        this.analyzer = new Analyzer(contextualTerms, passportUS);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        return findSpans(filterProfile, analyzer, input, context, documentId);

    }

}