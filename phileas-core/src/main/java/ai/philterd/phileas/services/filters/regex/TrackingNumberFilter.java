/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.model.policy.Policy;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TrackingNumberFilter extends RegexFilter {

    public TrackingNumberFilter(FilterConfiguration filterConfiguration, final boolean ups, final boolean fedex, final boolean usps) {
        super(FilterType.TRACKING_NUMBER, filterConfiguration);

        // https://andrewkurochkin.com/blog/code-for-recognizing-delivery-company-by-track

        final List<FilterPattern> filterPatterns = new LinkedList<>();

        if(fedex) {

            // FedEx
            final Pattern fedex1 = Pattern.compile("\\b[0-9]{20}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern fedex1FilterPattern = new FilterPattern.FilterPatternBuilder(fedex1, 0.75).withClassification("fedex").build();
            filterPatterns.add(fedex1FilterPattern);

            final Pattern fedex2 = Pattern.compile("\\b[0-9]{15}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern fedex2FilterPattern = new FilterPattern.FilterPatternBuilder(fedex2, 0.75).withClassification("fedex").build();
            filterPatterns.add(fedex2FilterPattern);

            final Pattern fedex3 = Pattern.compile("\\b[0-9]{12}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern fedex3FilterPattern = new FilterPattern.FilterPatternBuilder(fedex3, 0.75).withClassification("fedex").build();
            filterPatterns.add(fedex3FilterPattern);

            final Pattern fedex4 = Pattern.compile("\\b[0-9]{22}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern fedex4FilterPattern = new FilterPattern.FilterPatternBuilder(fedex4, 0.75).withClassification("fedex").build();
            filterPatterns.add(fedex4FilterPattern);

        }

        if(ups) {

            // UPS

            final Pattern ups1 = Pattern.compile("\\b(1Z)[0-9A-Z]{16}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern ups1FilterPattern = new FilterPattern.FilterPatternBuilder(ups1, 0.90).withClassification("ups").build();
            filterPatterns.add(ups1FilterPattern);

            final Pattern ups2 = Pattern.compile("\\b(T)+[0-9A-Z]{10}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern ups2FilterPattern = new FilterPattern.FilterPatternBuilder(ups2, 0.90).withClassification("ups").build();
            filterPatterns.add(ups2FilterPattern);

            final Pattern ups3 = Pattern.compile("\\b[0-9]{9}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern ups3FilterPattern = new FilterPattern.FilterPatternBuilder(ups3, 0.75).withClassification("ups").build();
            filterPatterns.add(ups3FilterPattern);

            final Pattern ups4 = Pattern.compile("\\b[0-9]{26}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern ups4FilterPattern = new FilterPattern.FilterPatternBuilder(ups4, 0.75).withClassification("ups").build();
            filterPatterns.add(ups4FilterPattern);

        }

        if(usps) {

            // USPS

            final Pattern usps1 = Pattern.compile("\\b(94|93|92|94|95)[0-9]{20}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps1FilterPattern = new FilterPattern.FilterPatternBuilder(usps1, 0.90).withClassification("usps").build();
            filterPatterns.add(usps1FilterPattern);

            final Pattern usps2 = Pattern.compile("\\b(94|93|92|94|95)[0-9]{22}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps2FilterPattern = new FilterPattern.FilterPatternBuilder(usps2, 0.90).withClassification("usps").build();
            filterPatterns.add(usps2FilterPattern);

            final Pattern usps3 = Pattern.compile("\\b(70|14|23|03)[0-9]{14}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps3FilterPattern = new FilterPattern.FilterPatternBuilder(usps3, 0.90).withClassification("usps").build();
            filterPatterns.add(usps3FilterPattern);

            final Pattern usps4 = Pattern.compile("\\b([A-Z]{2})[0-9]{9}([A-Z]{2})\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps4FilterPattern = new FilterPattern.FilterPatternBuilder(usps4, 0.90).withClassification("usps").build();
            filterPatterns.add(usps4FilterPattern);

            final Pattern usps5 = Pattern.compile("\\b[0-9]{34}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps5FilterPattern = new FilterPattern.FilterPatternBuilder(usps5, 0.75).withClassification("usps").build();
            filterPatterns.add(usps5FilterPattern);

            final Pattern usps6 = Pattern.compile("\\b[0-9]{30}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps6FilterPattern = new FilterPattern.FilterPatternBuilder(usps6, 0.75).withClassification("usps").build();
            filterPatterns.add(usps6FilterPattern);

            final Pattern usps7 = Pattern.compile("\\b[0-9]{28}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps7FilterPattern = new FilterPattern.FilterPatternBuilder(usps7, 0.75).withClassification("usps").build();
            filterPatterns.add(usps7FilterPattern);

            final Pattern usps8 = Pattern.compile("\\b[0-9]{26}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps8FilterPattern = new FilterPattern.FilterPatternBuilder(usps8, 0.75).withClassification("usps").build();
            filterPatterns.add(usps8FilterPattern);

        }

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("tracking");
        this.contextualTerms.add("shipment");
        this.contextualTerms.add("shipping");
        this.contextualTerms.add("mailing");
        this.contextualTerms.add("sent");
        this.contextualTerms.add("delivered");

        this.analyzer = new Analyzer(contextualTerms, filterPatterns);

    }

    @Override
    public FilterResult filter(Policy policy, String context, String documentId, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context, documentId, attributes);

        return new FilterResult(context, documentId, spans);

    }

}
