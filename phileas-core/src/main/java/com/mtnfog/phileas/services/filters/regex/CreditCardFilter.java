package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

public class CreditCardFilter extends RegexFilter implements Serializable {

    private boolean onlyValidCreditCardNumbers;
    private LuhnCheckDigit luhnCheckDigit;

    public CreditCardFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, boolean onlyValidCreditCardNumbers, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.CREDIT_CARD, strategies, anonymizationService, ignored, crypto, windowSize);

        this.onlyValidCreditCardNumbers = onlyValidCreditCardNumbers;
        this.luhnCheckDigit = new LuhnCheckDigit();

        // See http://regular-expressions.info/creditcard.html
        final Pattern CREDIT_CARD_REGEX = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern creditcard1 = new FilterPattern(CREDIT_CARD_REGEX, 0.90);

        this.contextualTerms = new HashSet<>(){{
            add("credit");
            add("card");
            add("american express");
            add("amex");
            add("discover");
            add("jcb");
            add("diners");
        }};

        this.analyzer = new Analyzer(contextualTerms, creditcard1);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

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
