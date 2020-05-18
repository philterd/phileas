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
import org.apache.commons.validator.routines.IBANValidator;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

public class IbanCodeFilter extends RegexFilter implements Serializable {

    private boolean validate;

    public IbanCodeFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, Set<String> ignored, Crypto crypto, boolean validate, int windowSize) {
        super(FilterType.IBAN_CODE, strategies, anonymizationService, ignored, crypto, windowSize);

        // Whether or not to validate the found IBAN codes.
        this.validate = validate;

        // https://stackoverflow.com/q/44656264
        final Pattern IBAN_REGEX = Pattern.compile("\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern iban1 = new FilterPattern(IBAN_REGEX, 0.90);

        this.contextualTerms = new HashSet<>(){{
            add("iban");
            add("bank");
        }};

        this.analyzer = new Analyzer(contextualTerms, iban1);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        final List<Span> validSpans = new LinkedList<>();

        if(validate) {

            // Validate the IBANs.
            // commons-validator has an IBANValidator class.
            // https://commons.apache.org/proper/commons-validator/apidocs/src-html/org/apache/commons/validator/routines/IBANValidator.html

            for (final Span span : spans) {

                final boolean valid = IBANValidator.getInstance().isValid(span.getText());

                if(valid) {
                    validSpans.add(span);
                }

            }

        } else {

            validSpans.addAll(spans);

        }

        return spans;

    }

}
