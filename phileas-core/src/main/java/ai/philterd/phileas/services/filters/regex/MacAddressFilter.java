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
import ai.philterd.phileas.model.profile.Crypto;
import ai.philterd.phileas.model.profile.FilterProfile;
import ai.philterd.phileas.model.profile.IgnoredPattern;
import ai.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.model.services.AnonymizationService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MacAddressFilter extends RegexFilter {

    public MacAddressFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.MAC_ADDRESS, filterConfiguration);

        final Pattern macAddressPattern = Pattern.compile("\\b([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})\\b");
        final FilterPattern macAddress1 = new FilterPattern.FilterPatternBuilder(macAddressPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("mac");
        this.contextualTerms.add("network");

        this.analyzer = new Analyzer(contextualTerms, macAddress1);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}
