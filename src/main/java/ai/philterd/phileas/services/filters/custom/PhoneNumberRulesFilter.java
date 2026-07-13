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
package ai.philterd.phileas.services.filters.custom;

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.RulesFilter;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.PhoneNumber;
import ai.philterd.phileas.services.context.ContextService;
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

// TODO: This should not extend RulesFilter because it is not a rule-based filter.

public class PhoneNumberRulesFilter extends RulesFilter {

    private final PhoneNumberUtil phoneUtil;
    private final List<String> regions;
    private final Pattern pattern = Pattern.compile("^(\\+\\d{1,2}\\s)?\\(?\\d{3}\\)?[\\s.-]\\d{3}[\\s.-]\\d{4}$");

    public PhoneNumberRulesFilter(final FilterConfiguration filterConfiguration) {
        this(filterConfiguration, List.of(PhoneNumber.DEFAULT_REGION));
    }

    public PhoneNumberRulesFilter(final FilterConfiguration filterConfiguration, final List<String> regions) {

        super(FilterType.PHONE_NUMBER, filterConfiguration);

        this.phoneUtil = PhoneNumberUtil.getInstance();
        this.regions = (regions == null || regions.isEmpty()) ? List.of(PhoneNumber.DEFAULT_REGION) : regions;

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("phone");
        this.contextualTerms.add("telephone");
        this.contextualTerms.add("fax");
        this.contextualTerms.add("cell");
        this.contextualTerms.add("mobile");

    }

    @Override
    public Filtered filter(final ContextService contextService, final Policy policy, final String context, final int piece,
                           final String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        if(policy.getIdentifiers().hasFilter(filterType)) {

            // Scan once per configured region and merge the results. A "+"-prefixed number is found under
            // every region and a bare national-format number may match in several, so overlapping spans are
            // de-duplicated below, preferring valid numbers over merely-possible ones and longer over shorter.
            final List<PhoneNumberMatch> allMatches = new ArrayList<>();
            for (final String region : regions) {
                for (final PhoneNumberMatch match : phoneUtil.findNumbers(input, region, PhoneNumberUtil.Leniency.POSSIBLE, Long.MAX_VALUE)) {
                    allMatches.add(match);
                }
            }

            for (final PhoneNumberMatch match : dedupe(allMatches)) {

                final String text = match.rawString();

                // Is it formatted like a phone number?
                double confidence;
                if(text.matches(pattern.pattern())) {
                    confidence = 0.95;
                } else{
                    if(text.length() > 14) {
                        confidence = 0.75;
                    } else {
                        confidence = 0.60;
                    }
                }

                final String[] window = getWindow(input, match.start(), match.end());
                final String classification = "";
                final Replacement replacement = getReplacement(contextService, policy, context, text, window, confidence,
                        classification, null);
                final boolean isIgnored = ignored.contains(text);

                spans.add(Span.make(match.start(), match.end(), getFilterType(), context, confidence,
                        text, replacement.getReplacement(), replacement.getSalt(), isIgnored, replacement.isApplied(), window, priority));

            }

        }

        return new Filtered(context, spans);

    }

    /**
     * Merges matches found across the configured regions, removing overlapping spans. When two matches
     * overlap the "better" one is kept: a valid number is preferred over a merely-possible one, and among
     * equally-valid matches the longer span wins.
     */
    private List<PhoneNumberMatch> dedupe(final List<PhoneNumberMatch> matches) {

        // Best-first ordering so a greedy sweep keeps the strongest match in each overlapping cluster.
        final List<PhoneNumberMatch> sorted = new ArrayList<>(matches);
        sorted.sort((a, b) -> {
            final boolean aValid = phoneUtil.isValidNumber(a.number());
            final boolean bValid = phoneUtil.isValidNumber(b.number());
            if (aValid != bValid) {
                return aValid ? -1 : 1;
            }
            final int aLength = a.end() - a.start();
            final int bLength = b.end() - b.start();
            if (aLength != bLength) {
                return Integer.compare(bLength, aLength);
            }
            return Integer.compare(a.start(), b.start());
        });

        final List<PhoneNumberMatch> kept = new ArrayList<>();
        for (final PhoneNumberMatch candidate : sorted) {
            boolean overlaps = false;
            for (final PhoneNumberMatch accepted : kept) {
                if (candidate.start() < accepted.end() && accepted.start() < candidate.end()) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                kept.add(candidate);
            }
        }

        // Restore document order so emitted spans are left-to-right.
        kept.sort((a, b) -> Integer.compare(a.start(), b.start()));

        return kept;

    }

}
