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
import ai.philterd.phileas.services.context.ContextService;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class TrackingNumberFilter extends RegexFilter {

    public TrackingNumberFilter(FilterConfiguration filterConfiguration, final boolean ups, final boolean fedex, final boolean usps) {
        this(filterConfiguration, ups, fedex, usps, false);
    }

    /**
     * Creates a new tracking number filter.
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     * @param ups Whether to detect UPS tracking numbers.
     * @param fedex Whether to detect FedEx tracking numbers.
     * @param usps Whether to detect USPS tracking numbers.
     * @param allowSpaces Whether to also detect a tracking number written in space-separated groups.
     */
    public TrackingNumberFilter(FilterConfiguration filterConfiguration, final boolean ups, final boolean fedex, final boolean usps,
                                final boolean allowSpaces) {
        super(FilterType.TRACKING_NUMBER, filterConfiguration);

        // With spaces allowed, a single space may sit between any two characters of the number, so a
        // number printed in groups is matched whole. The number still starts and ends on a character.
        final String sp = allowSpaces ? " ?" : "";

        // https://andrewkurochkin.com/blog/code-for-recognizing-delivery-company-by-track

        final List<FilterPattern> filterPatterns = new LinkedList<>();

        if(fedex) {

            // FedEx
            final Pattern fedex1 = Pattern.compile("\\b" + run("\\d", 20, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern fedex1FilterPattern = new FilterPattern.FilterPatternBuilder(fedex1, 0.75).withClassification("fedex").build();
            filterPatterns.add(fedex1FilterPattern);

            final Pattern fedex2 = Pattern.compile("\\b" + run("\\d", 15, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern fedex2FilterPattern = new FilterPattern.FilterPatternBuilder(fedex2, 0.75).withClassification("fedex").build();
            filterPatterns.add(fedex2FilterPattern);

            final Pattern fedex3 = Pattern.compile("\\b" + run("\\d", 12, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern fedex3FilterPattern = new FilterPattern.FilterPatternBuilder(fedex3, 0.75).withClassification("fedex").build();
            filterPatterns.add(fedex3FilterPattern);

            final Pattern fedex4 = Pattern.compile("\\b" + run("\\d", 22, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern fedex4FilterPattern = new FilterPattern.FilterPatternBuilder(fedex4, 0.75).withClassification("fedex").build();
            filterPatterns.add(fedex4FilterPattern);

        }

        if(ups) {

            // UPS

            final Pattern ups1 = Pattern.compile("\\b(1Z)" + sp + run("[\\dA-Z]", 16, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern ups1FilterPattern = new FilterPattern.FilterPatternBuilder(ups1, 0.90).withClassification("ups").build();
            filterPatterns.add(ups1FilterPattern);

            // Digits only after the T, so an 11-letter word starting with "t" is not matched.
            final Pattern ups2 = Pattern.compile("\\bT" + sp + run("\\d", 10, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern ups2FilterPattern = new FilterPattern.FilterPatternBuilder(ups2, 0.90).withClassification("ups").build();
            filterPatterns.add(ups2FilterPattern);

            // No pattern for a bare 9-digit number: it cannot be told apart from an order, invoice or
            // account number, or an SSN written without dashes.

            final Pattern ups4 = Pattern.compile("\\b" + run("\\d", 26, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern ups4FilterPattern = new FilterPattern.FilterPatternBuilder(ups4, 0.75).withClassification("ups").build();
            filterPatterns.add(ups4FilterPattern);

        }

        if(usps) {

            // USPS

            final Pattern usps1 = Pattern.compile("\\b(94|93|92|94|95)" + sp + run("\\d", 20, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps1FilterPattern = new FilterPattern.FilterPatternBuilder(usps1, 0.90).withClassification("usps").build();
            filterPatterns.add(usps1FilterPattern);

            final Pattern usps2 = Pattern.compile("\\b(94|93|92|94|95)" + sp + run("\\d", 22, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps2FilterPattern = new FilterPattern.FilterPatternBuilder(usps2, 0.90).withClassification("usps").build();
            filterPatterns.add(usps2FilterPattern);

            // The same "94/93/92/95"-prefixed number is often written in space-separated groups of
            // four digits (e.g. "9400 1000 0000 0000 0000"). Match that grouped form as a single span.
            final Pattern usps2Grouped = Pattern.compile("\\b(94|93|92|95)\\d{2}( \\d{4}){4}\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps2GroupedFilterPattern = new FilterPattern.FilterPatternBuilder(usps2Grouped, 0.90).withClassification("usps").build();
            filterPatterns.add(usps2GroupedFilterPattern);

            final Pattern usps3 = Pattern.compile("\\b(70|14|23|03)" + sp + run("\\d", 14, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps3FilterPattern = new FilterPattern.FilterPatternBuilder(usps3, 0.90).withClassification("usps").build();
            filterPatterns.add(usps3FilterPattern);

            final Pattern usps4 = Pattern.compile("\\b([A-Z]{2})" + sp + run("\\d", 9, allowSpaces) + sp + "([A-Z]{2})\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps4FilterPattern = new FilterPattern.FilterPatternBuilder(usps4, 0.90).withClassification("usps").build();
            filterPatterns.add(usps4FilterPattern);

            final Pattern usps5 = Pattern.compile("\\b" + run("\\d", 34, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps5FilterPattern = new FilterPattern.FilterPatternBuilder(usps5, 0.75).withClassification("usps").build();
            filterPatterns.add(usps5FilterPattern);

            final Pattern usps6 = Pattern.compile("\\b" + run("\\d", 30, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps6FilterPattern = new FilterPattern.FilterPatternBuilder(usps6, 0.75).withClassification("usps").build();
            filterPatterns.add(usps6FilterPattern);

            final Pattern usps7 = Pattern.compile("\\b" + run("\\d", 28, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
            final FilterPattern usps7FilterPattern = new FilterPattern.FilterPatternBuilder(usps7, 0.75).withClassification("usps").build();
            filterPatterns.add(usps7FilterPattern);

            final Pattern usps8 = Pattern.compile("\\b" + run("\\d", 26, allowSpaces) + "\\b", Pattern.CASE_INSENSITIVE);
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

    /**
     * Returns a pattern for a run of characters. With spaces allowed, a single space may separate
     * any two of them.
     * @param characterClass The pattern for one character.
     * @param length The number of characters.
     * @param allowSpaces Whether a space may separate the characters.
     * @return The pattern.
     */
    private static String run(final String characterClass, final int length, final boolean allowSpaces) {
        return allowSpaces
                ? characterClass + "(?: ?" + characterClass + "){" + (length - 1) + "}"
                : characterClass + "{" + length + "}";
    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        return new Filtered(context, spans);

    }

}
