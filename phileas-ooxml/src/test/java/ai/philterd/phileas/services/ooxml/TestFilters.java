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
package ai.philterd.phileas.services.ooxml;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.Explanation;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Filters for tests: real Phileas policies, and hand-built results. */
final class TestFilters {

    /** Email addresses, redacted to {@code {{{REDACTED-email-address}}}}. */
    static final String EMAIL_POLICY = "{\"identifiers\":{\"emailAddress\":{}}}";
    /** First names and surnames. */
    static final String NAME_POLICY = "{\"identifiers\":{\"firstName\":{},\"surname\":{}}}";
    /** Social Security numbers. */
    static final String SSN_POLICY = "{\"identifiers\":{\"ssn\":{}}}";

    /** A filter for a policy given as Phileas policy JSON, with its own in-memory context. */
    static TextFilter forPolicy(final String policyJson) {
        final Policy policy = new Gson().fromJson(policyJson, Policy.class);
        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
        return text -> {
            try {
                return service.filter(policy, "ctx", text);
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    static TextFilter email() {
        return forPolicy(EMAIL_POLICY);
    }

    /** A custom identifier policy: {@code pattern} redacted with classification {@code classification}. */
    static TextFilter customIdentifier(final String pattern, final String classification) {
        final String json = "{\"identifiers\":{\"identifiers\":[{\"pattern\":\"" + pattern.replace("\\", "\\\\")
                + "\",\"classification\":\"" + classification + "\"}]}}";
        return forPolicy(json);
    }

    /** A filter that wraps {@code inner} and records every text it is given. */
    static TextFilter recording(final TextFilter inner, final List<String> seen) {
        return text -> {
            seen.add(text);
            return inner.apply(text);
        };
    }

    /** A filter that changes nothing. */
    static TextFilter none() {
        return text -> result(text, List.of());
    }

    /** A hand-built result, as a test's own filter delegate returns. */
    static TextFilterResult result(final String filteredText, final List<Span> spans) {
        final List<Span> applied = new ArrayList<>(spans);
        return new TextFilterResult(filteredText, "ctx", 0, new Explanation(applied, applied), new ArrayList<>(), 0);
    }

    /** A detection over [start, end) of {@code text} replaced by {@code replacement}. */
    static Span span(final String text, final int start, final int end, final String replacement) {
        final Span span = Span.make(start, end, FilterType.CUSTOM_DICTIONARY, "ctx", 1.0,
                text.substring(start, end), replacement, "", false, true, new String[0], 0);
        span.setClassification("custom");
        return span;
    }

    /** A filter that replaces every occurrence of {@code value} with {@code replacement}. */
    static TextFilter replacing(final String value, final String replacement) {
        return text -> {
            final List<Span> spans = new ArrayList<>();
            int index = 0;
            while ((index = text.indexOf(value, index)) >= 0) {
                spans.add(span(text, index, index + value.length(), replacement));
                index += value.length();
            }
            return result(text.replace(value, replacement), spans);
        };
    }

    private TestFilters() {
    }

}
