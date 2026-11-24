/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.Analyzer;
import ai.philterd.phileas.services.context.ContextService;

import java.util.HashSet;
import java.util.List;

/**
 * Detects health plan beneficiary and member IDs, which have no common format, when they follow a cue
 * word such as {@code Member ID}. An overlapping MBI, SSN, NPI, phone number, or date takes priority.
 */
public class HealthPlanMemberIdFilter extends CueWordFilter {

    /** 6 to 20 letters, digits, and hyphens, with at least one digit. */
    public static final String DEFAULT_PATTERN =
            "(?<![A-Za-z0-9-])(?=[A-Za-z0-9-]{0,19}[0-9])[A-Za-z0-9-]{6,20}(?![A-Za-z0-9-])";

    public static final List<String> DEFAULT_CUE_WORDS = List.of("member ID", "member #", "member number",
            "subscriber ID", "subscriber #", "subscriber number", "beneficiary ID", "Medicaid ID", "insurance ID");

    public HealthPlanMemberIdFilter(final FilterConfiguration filterConfiguration) {
        this(filterConfiguration, null, null, true);
    }

    public HealthPlanMemberIdFilter(final FilterConfiguration filterConfiguration, final List<String> patterns,
                                    final List<String> cueWords, final boolean defaultCueWords) {
        super(FilterType.HEALTH_PLAN_MEMBER_ID, filterConfiguration, DEFAULT_CUE_WORDS, cueWords, defaultCueWords);

        this.contextualTerms = new HashSet<>();
        this.analyzer = new Analyzer(contextualTerms, valuePatterns(DEFAULT_PATTERN, patterns));

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);
        spans.forEach(span -> foundByCueWord(input, span));

        // The policy's patterns may overlap one another.
        return new Filtered(context, Span.dropOverlappingSpans(spans));

    }

    @Override
    protected boolean accepts(final String input, final int characterStart, final int characterEnd,
                              final FilterPattern filterPattern) {
        return followsCueWord(input, characterStart);
    }

}
