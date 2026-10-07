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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.mockito.Mockito.mock;

/**
 * The phone number filter's own ignored terms and ignored patterns apply to the matched number.
 */
class PhoneNumberIgnoredTest {

    private static final String KEPT = "(555) 555-0100";
    private static final String OTHER = "(212) 555-0199";
    private static final String INPUT = "Call " + KEPT + " or " + OTHER + " now.";

    private final VectorService vectorService = mock(VectorService.class);

    @Test
    void anIgnoredTermIsNotRedacted() throws Exception {
        assertOnlyOtherRedacted("\"ignored\": [\"" + KEPT + "\"]");
    }

    @Test
    void anIgnoredPatternMatchingTheNumberIsNotRedacted() throws Exception {
        assertOnlyOtherRedacted("\"ignoredPatterns\": [{\"pattern\": \"\\\\(555\\\\).*\"}]");
    }

    @Test
    void aPatternMatchingOnlyPartOfTheNumberIgnoresNothing() throws Exception {
        assertBothRedacted("\"ignoredPatterns\": [{\"pattern\": \"\\\\(555\\\\)\"}]");
    }

    @Test
    void aPatternMatchingTheWholeInputButNotTheNumberIgnoresNothing() throws Exception {
        assertBothRedacted("\"ignoredPatterns\": [{\"pattern\": \"Call.*now\\\\.\"}]");
    }

    @Test
    void withoutAnIgnoreListBothNumbersAreRedacted() throws Exception {
        assertBothRedacted(null);
    }

    private void assertOnlyOtherRedacted(final String ignore) throws Exception {

        final TextFilterResult result = filter(ignore);
        final String filtered = result.getFilteredText();

        Assertions.assertTrue(filtered.contains(KEPT), filtered);
        Assertions.assertFalse(filtered.contains(OTHER), filtered);

        final Span span = result.getExplanation().identifiedSpans().stream()
                .filter(s -> KEPT.equals(s.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the number should still be identified"));

        Assertions.assertTrue(span.isIgnored());

    }

    private void assertBothRedacted(final String ignore) throws Exception {

        final String filtered = filter(ignore).getFilteredText();

        Assertions.assertFalse(filtered.contains(KEPT), filtered);
        Assertions.assertFalse(filtered.contains(OTHER), filtered);

    }

    private TextFilterResult filter(final String ignore) throws Exception {

        final String json = "{ \"identifiers\": { \"phoneNumber\": { " + (ignore == null ? "" : ignore) + " } } }";
        final Policy policy = new Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), vectorService, null);

        return service.filter(policy, "context", INPUT);

    }

}
