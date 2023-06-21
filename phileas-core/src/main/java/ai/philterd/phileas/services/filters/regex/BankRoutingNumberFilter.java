/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.regex.RegexFilter;
import ai.philterd.phileas.model.objects.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.FilterProfile;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class BankRoutingNumberFilter extends RegexFilter {

    public BankRoutingNumberFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.BANK_ROUTING_NUMBER, filterConfiguration);

        final Pattern routingNumberPattern = Pattern.compile("\\b[0-9]{9}\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern routingNumber = new FilterPattern.FilterPatternBuilder(routingNumberPattern, 0.95).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("routing");
        this.contextualTerms.add("bank");

        this.analyzer = new Analyzer(contextualTerms, routingNumber);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        final List<Span> validSpans = new LinkedList<>();

        for(final Span span : nonOverlappingSpans) {

            final String digits = span.getText();

            final int checksum =
                    (3 * (Character.getNumericValue(digits.charAt(0)) + Character.getNumericValue(digits.charAt(3)) + Character.getNumericValue(digits.charAt(6))) +
                    7 * (Character.getNumericValue(digits.charAt(1)) + Character.getNumericValue(digits.charAt(4)) + Character.getNumericValue(digits.charAt(7))) +
                    1 * (Character.getNumericValue(digits.charAt(2)) + Character.getNumericValue(digits.charAt(5)) + Character.getNumericValue(digits.charAt(8)))) % 10;

            if(checksum == 0) {
                validSpans.add(span);
            }

        }

        return new FilterResult(context, documentId, validSpans);

    }

}
