/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.services.filters.regex;

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.regex.RegexFilter;
import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.Analyzer;
import org.apache.commons.validator.routines.IBANValidator;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class IbanCodeFilter extends RegexFilter {

    private final boolean validate;

    public IbanCodeFilter(FilterConfiguration filterConfiguration, boolean validate, boolean allowSpaces) {
        super(FilterType.IBAN_CODE, filterConfiguration);

        // Whether to validate the found IBAN codes.
        this.validate = validate;

        // PHL-139: Allow spaces in IBAN Codes.
        // It is important to note that there are no spaces in an IBAN when transmitted electronically.
        // When printed it is expressed in groups of four characters separated by a single space, the last group being of variable length.
        // https://fexco.com/fexco/news/swift-bic-iban-explained/

        // See https://stackoverflow.com/q/44656264
        final Pattern ibanPattern;

        if(allowSpaces) {
            ibanPattern = Pattern.compile("\\b[A-Z]{2}[0-9]{2}[\\s]?[A-Z0-9]{4}[\\s]{0,1}[A-Z0-9]{4}[\\s]?[A-Z0-9]{4}[\\s]?[A-Z0-9]{4}[\\s]?[A-Z0-9]{2}\\b", Pattern.CASE_INSENSITIVE);
        } else {
            ibanPattern = Pattern.compile("\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}\\b", Pattern.CASE_INSENSITIVE);
        }

        final FilterPattern iban = new FilterPattern.FilterPatternBuilder(ibanPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("iban");
        this.contextualTerms.add("bank");

        this.analyzer = new Analyzer(contextualTerms, iban);

    }

    @Override
    public Filtered filter(Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context);

        final List<Span> validSpans = new LinkedList<>();

        if(validate) {

            // Validate the IBANs.
            // commons-validator has an IBANValidator class.
            // https://commons.apache.org/proper/commons-validator/apidocs/src-html/org/apache/commons/validator/routines/IBANValidator.html

            for (final Span span : spans) {

                final boolean valid = IBANValidator.getInstance().isValid(span.getText().replaceAll("\\s", ""));

                if(valid) {
                    validSpans.add(span);
                }

            }

        } else {

            validSpans.addAll(spans);

        }

        return new Filtered(context, validSpans);

    }

}
