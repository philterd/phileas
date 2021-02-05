package com.mtnfog.phileas.services.filters.custom;

import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.RulesFilter;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.IgnoredPattern;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

// TODO: This should not extend RulesFilter because it is not a rule-based filter.

public class PhoneNumberRulesFilter extends RulesFilter {

    private final PhoneNumberUtil phoneUtil;
    private final Pattern pattern = Pattern.compile("^(\\+\\d{1,2}\\s)?\\(?\\d{3}\\)?[\\s.-]\\d{3}[\\s.-]\\d{4}$");

    public PhoneNumberRulesFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Set<String> ignoredFiles, List<IgnoredPattern> ignoredPatterns, Crypto crypto, int windowSize) {

        super(FilterType.PHONE_NUMBER, strategies, anonymizationService, alertService, ignored, ignoredFiles, ignoredPatterns, crypto, windowSize);

        this.phoneUtil = PhoneNumberUtil.getInstance();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("phone");
        this.contextualTerms.add("telephone");
        this.contextualTerms.add("fax");
        this.contextualTerms.add("cell");
        this.contextualTerms.add("mobile");

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        if(filterProfile.getIdentifiers().hasFilter(filterType)) {

            final Iterable<PhoneNumberMatch> matches = phoneUtil.findNumbers(input, "US", PhoneNumberUtil.Leniency.POSSIBLE, 1);

            for (final PhoneNumberMatch match : matches) {

                final String text = match.rawString();

                // Is it formatted like a phone number?
                double confidence = 0.0;
                if(text.matches(pattern.pattern())) {
                    confidence = 0.95;
                } else{
                    if(text.length() > 14) {
                        confidence = 0.75;
                    } else {
                        confidence = 0.60;
                    }
                }

                final String[] window = getWindow(input, match.start(), match.end());
                final String classification = "";
                final Replacement replacement = getReplacement(filterProfile.getName(), context, documentId, text, window, confidence, classification, null);
                final boolean isIgnored = ignored.contains(text);

                spans.add(Span.make(match.start(), match.end(), getFilterType(), context, documentId, confidence, text, replacement.getReplacement(), replacement.getSalt(), isIgnored, window));

            }

        }

        return new FilterResult(context, documentId, spans);

    }

}
