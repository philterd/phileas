package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class CreditCardFilter extends RegexFilter implements Serializable {

    // See http://regular-expressions.info/creditcard.html
    private static final Pattern ID_REGEX = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");

    private boolean onlyValidCreditCardNumbers;
    private LuhnCheckDigit luhnCheckDigit;

    public CreditCardFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, boolean onlyValidCreditCardNumbers, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.CREDIT_CARD, strategies, anonymizationService, ignored, crypto, windowSize);

        this.onlyValidCreditCardNumbers = onlyValidCreditCardNumbers;
        this.luhnCheckDigit = new LuhnCheckDigit();

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, ID_REGEX, input, context, documentId);

        final List<Span> validSpans = new LinkedList<>();

        for(final Span span : spans) {

            final String creditCardNumber = input.substring(span.getCharacterStart(), span.getCharacterEnd());

            if(onlyValidCreditCardNumbers && luhnCheckDigit.isValid(creditCardNumber)) {
                validSpans.add(span);
            } else {
                validSpans.add(span);
            }

        }

        return validSpans;

    }

}
