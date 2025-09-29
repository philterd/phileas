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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.regex.RegexFilter;
import ai.philterd.phileas.model.objects.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.policy.Policy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class EmailAddressFilter extends RegexFilter {

    private final boolean onlyValidTLDs;

    private Collection<String> tlds = null;

    public EmailAddressFilter(FilterConfiguration filterConfiguration, boolean onlyStrictMatches, boolean onlyValidTLDs) throws IOException {
        super(FilterType.EMAIL_ADDRESS, filterConfiguration);

        final Pattern emailAddressPattern = onlyStrictMatches
                ? Pattern.compile("\\b(?:[a-z\\d!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z\\d!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z\\d](?:[a-z\\d-]*[a-z\\d])?\\.)+[a-z\\d](?:[a-z\\d-]*[a-z\\d])?|\\[(?:(?:25[0-5]|2[0-4][\\d]|[01]?[\\d][\\d]?)\\.){3}(?:25[0-5]|2[0-4][\\d]|[01]?[\\d][\\d]?|[a-z\\d-]*[a-z\\d]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\b])", Pattern.CASE_INSENSITIVE)
                : Pattern.compile("\\b[\\w.-]+?@(?:([a-zA-Z\\d\\-])+?\\.)+(?:[a-zA-Z\\d]{2,4})+\\b");

        final FilterPattern email1 = new FilterPattern.FilterPatternBuilder(emailAddressPattern, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("email");
        this.contextualTerms.add("e-mail");

        this.analyzer = new Analyzer(contextualTerms, email1);
        this.onlyValidTLDs = onlyValidTLDs;

        if(onlyValidTLDs) {
            final File file = new File(getClass().getClassLoader().getResource("tlds-alpha-by-domain.txt").getFile());
            final List<String> rawTlds = Files.readAllLines(file.toPath(), Charset.defaultCharset());
            this.tlds = rawTlds.stream().filter(str->!str.startsWith("#")).map(String::toLowerCase).map(s -> "." + s).toList();
        }

    }

    @Override
    public FilterResult filter(Policy policy, String contextName, String documentId, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, contextName, documentId, attributes);

        if(onlyValidTLDs) {
            spans.removeIf(str -> tlds.stream().noneMatch(str.getText()::endsWith));
        }

        return new FilterResult(contextName, documentId, spans);

    }

}
