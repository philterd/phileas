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

import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.RulesFilter;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

// TODO: This should not extend RulesFilter because it is not a rule-based filter.

public class PhoneNumberRulesFilter extends RulesFilter {

    private final PhoneNumberUtil phoneUtil;
    private final Pattern pattern = Pattern.compile("^(\\+\\d{1,2}\\s)?\\(?\\d{3}\\)?[\\s.-]\\d{3}[\\s.-]\\d{4}$");

    public PhoneNumberRulesFilter(final FilterConfiguration filterConfiguration) {

        super(FilterType.PHONE_NUMBER, filterConfiguration);

        this.phoneUtil = PhoneNumberUtil.getInstance();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("phone");
        this.contextualTerms.add("telephone");
        this.contextualTerms.add("fax");
        this.contextualTerms.add("cell");
        this.contextualTerms.add("mobile");

    }

    @Override
    public Filtered filter(final Policy policy, final String context, final int piece,
                           final String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        if(policy.getIdentifiers().hasFilter(filterType)) {

            final Iterable<PhoneNumberMatch> matches = phoneUtil.findNumbers(input, "US", PhoneNumberUtil.Leniency.POSSIBLE, 1);

            for (final PhoneNumberMatch match : matches) {

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
                final Replacement replacement = getReplacement(policy, context, text, window, confidence,
                        classification, null);
                final boolean isIgnored = ignored.contains(text);

                spans.add(Span.make(match.start(), match.end(), getFilterType(), context, confidence,
                        text, replacement.getReplacement(), replacement.getSalt(), isIgnored, replacement.isApplied(), window, priority));

            }

        }

        return new Filtered(context, spans);

    }

}
