package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.IgnoredPattern;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.commons.validator.routines.IBANValidator;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class IbanCodeFilter extends RegexFilter {

    private boolean validate;

    public IbanCodeFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, List<IgnoredPattern> ignoredPatterns, Crypto crypto, boolean validate, int windowSize) {
        super(FilterType.IBAN_CODE, strategies, anonymizationService, alertService, ignored, ignoredPatterns, crypto, windowSize);

        // Whether or not to validate the found IBAN codes.
        this.validate = validate;

        // PHL-139: Allow spaces in IBAN Codes.
        // It is important to note that there are no spaces in an IBAN when transmitted electronically.
        // When printed it is expressed in groups of four characters separated by a single space, the last group being of variable length.
        // https://fexco.com/fexco/news/swift-bic-iban-explained/

        // This pattern does not allow spaces to group in 4s. See https://stackoverflow.com/q/44656264
        // final Pattern ibanPattern = Pattern.compile("\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}\\b", Pattern.CASE_INSENSITIVE);
        final Pattern ibanPattern = Pattern.compile("\\b[A-Z]{2}[0-9]{2}[\\s]?[A-Z0-9]{4}[\\s]{0,1}[A-Z0-9]{4}[\\s]?[A-Z0-9]{4}[\\s]?[A-Z0-9]{4}[\\s]?[A-Z0-9]{2}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern iban = new FilterPattern.FilterPatternBuilder(ibanPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("iban");
        this.contextualTerms.add("bank");

        this.analyzer = new Analyzer(contextualTerms, iban);

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
