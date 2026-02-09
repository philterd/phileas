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
import ai.philterd.phileas.model.metadata.zipcode.ZipCodeMetadataRequest;
import ai.philterd.phileas.model.metadata.zipcode.ZipCodeMetadataResult;
import ai.philterd.phileas.model.metadata.zipcode.ZipCodeMetadataService;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.Analyzer;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class ZipCodeFilter extends RegexFilter {

    private final boolean validate;

    public ZipCodeFilter(FilterConfiguration filterConfiguration, boolean requireDelimiter, boolean validate) {
        super(FilterType.ZIP_CODE, filterConfiguration);

        this.validate = validate;

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("zip");
        this.contextualTerms.add("zipcode");
        this.contextualTerms.add("postal");

        if(requireDelimiter) {

            // With delimiter
            final Pattern zipCodePattern = Pattern.compile("\\b[0-9]{5}(?:-[0-9]{4})?\\b");
            final FilterPattern zipCode = new FilterPattern.FilterPatternBuilder(zipCodePattern, 0.90).build();

            this.analyzer = new Analyzer(contextualTerms, zipCode);

        } else {

            // Without delimiter.
            final Pattern zipCodePattern = Pattern.compile("\\b[0-9]{5}(?:-?[0-9]{4})?\\b");
            final FilterPattern zipCode = new FilterPattern.FilterPatternBuilder(zipCodePattern, 0.50).build();

            this.analyzer = new Analyzer(contextualTerms, zipCode);

        }

    }

    @Override
    public Filtered filter(Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context);

        if(validate) {

            final ZipCodeMetadataService zipCodeMetadataService = new ZipCodeMetadataService();

            for (final Span span : spans) {

                // Zip code database only has first 5 digits.
                final String zipCode = span.getText().substring(0, 5);

                final ZipCodeMetadataResult zipCodeMetadataResponse = zipCodeMetadataService.getMetadata(new ZipCodeMetadataRequest(zipCode));

                if (!zipCodeMetadataResponse.isExists()) {
                    span.setApplied(false);
                }

            }

        }

        return new Filtered(context, spans);

    }

}
