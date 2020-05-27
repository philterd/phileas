package com.mtnfog.phileas.services.filters.custom;

import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.RulesFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PhoneNumberRulesFilter extends RulesFilter implements Serializable {

    private PhoneNumberUtil phoneUtil;

    public PhoneNumberRulesFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Crypto crypto, int windowSize) {

        super(FilterType.PHONE_NUMBER, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

        this.phoneUtil = PhoneNumberUtil.getInstance();

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        if(filterProfile.getIdentifiers().hasFilter(filterType)) {

            final Iterable<PhoneNumberMatch> matches = phoneUtil.findNumbers(input, "US", PhoneNumberUtil.Leniency.POSSIBLE, Long.MAX_VALUE);

            for (PhoneNumberMatch match : matches) {

                final String text = match.rawString();
                final String replacement = getReplacement(filterProfile.getName(), label, context, documentId, text, Collections.emptyMap());
                final boolean isIgnored = ignored.contains(text);

                final String[] window = getWindow(input, match.start(), match.end());

                spans.add(Span.make(match.start(), match.end(), getFilterType(), context, documentId, 1.0, text, replacement, isIgnored, window));

            }

        }

        return spans;

    }

}
