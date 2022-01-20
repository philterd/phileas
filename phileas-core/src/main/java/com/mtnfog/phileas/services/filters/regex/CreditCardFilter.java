package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class CreditCardFilter extends RegexFilter {

    private boolean onlyValidCreditCardNumbers;
    private LuhnCheckDigit luhnCheckDigit;

    public CreditCardFilter(FilterConfiguration filterConfiguration, boolean onlyValidCreditCardNumbers) {
        super(FilterType.CREDIT_CARD, filterConfiguration);

        this.onlyValidCreditCardNumbers = onlyValidCreditCardNumbers;
        this.luhnCheckDigit = new LuhnCheckDigit();

        // See http://regular-expressions.info/creditcard.html
        final Pattern creditCardPattern = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern creditcard1 = new FilterPattern.FilterPatternBuilder(creditCardPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("credit");
        this.contextualTerms.add("card");
        this.contextualTerms.add("american express");
        this.contextualTerms.add("amex");
        this.contextualTerms.add("discover");
        this.contextualTerms.add("jcb");
        this.contextualTerms.add("diners");

        this.analyzer = new Analyzer(contextualTerms, creditcard1);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        final List<Span> validSpans = new LinkedList<>();

        for(final Span span : spans) {

            final String creditCardNumber = input.substring(span.getCharacterStart(), span.getCharacterEnd())
                    .replaceAll(" ", "")
                    .replaceAll("-", "");

            if(onlyValidCreditCardNumbers) {

                if(luhnCheckDigit.isValid(creditCardNumber)) {
                    validSpans.add(span);
                }

            } else {
                validSpans.add(span);
            }

        }

        return new FilterResult(context, documentId, validSpans);

    }

}
