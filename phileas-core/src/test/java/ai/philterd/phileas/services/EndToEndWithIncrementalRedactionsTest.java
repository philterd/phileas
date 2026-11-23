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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.IncrementalRedaction;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import ai.philterd.phileas.utils.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicy;
import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyWithSplits;

public class EndToEndWithIncrementalRedactionsTest {

    private static final Logger LOGGER = LogManager.getLogger(EndToEndWithIncrementalRedactionsTest.class);

    // Use the real services rather than mocks so these end-to-end tests actually exercise the
    // replacement-context and vector wiring instead of no-op stubs.
    private final ContextService contextService = new DefaultContextService();
    private final VectorService vectorService = new InMemoryVectorService();

    @Test
    public void endToEndWithRedactionIncrements() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "George Washington whose SSN was 123-45-6789 was the first president of the United States and he lived at 90210.");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington whose SSN was {{{REDACTED-ssn}}} was the first president of the United States and he lived at {{{REDACTED-zip-code}}}.", response.getFilteredText());
        Assertions.assertTrue(CollectionUtils.isNotEmpty(response.getIncrementalRedactions()));

        for(final IncrementalRedaction incrementalRedaction : response.getIncrementalRedactions()) {
            LOGGER.info("Incremental Redaction: {}", incrementalRedaction);
            Assertions.assertEquals(DigestUtils.sha256Hex(incrementalRedaction.getIncrementallyRedactedText()), incrementalRedaction.getHash());
        }

    }

    @Test
    public void incrementalRedactionsKeepTheirTextsAndHashes() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(properties), contextService, vectorService, null);
        final TextFilterResult response = service.filter(getPolicy(), "context", "George Washington whose SSN was 123-45-6789 was the first president of the United States and he lived at 90210.");

        // The texts and hashes from before the texts were built on request.
        final List<IncrementalRedaction> redactions = response.getIncrementalRedactions();
        Assertions.assertEquals(2, redactions.size());
        Assertions.assertEquals("George Washington whose SSN was {{{REDACTED-ssn}}} was the first president of the United States and he lived at 90210.", redactions.get(0).getIncrementallyRedactedText());
        Assertions.assertEquals("8f444b3c47479e532701a36ce68e45b3d691fa0b16d60415dcb658c418bcfdff", redactions.get(0).getHash());
        Assertions.assertEquals("123-45-6789", redactions.get(0).getSpan().getText());
        Assertions.assertEquals("George Washington whose SSN was {{{REDACTED-ssn}}} was the first president of the United States and he lived at {{{REDACTED-zip-code}}}.", redactions.get(1).getIncrementallyRedactedText());
        Assertions.assertEquals("7450758c6bfb49dd3ded2f495dd66320e4d25f7bd634e106bef89b7bf3cb9fa9", redactions.get(1).getHash());
        Assertions.assertEquals("90210", redactions.get(1).getSpan().getText());

    }

    @Test
    public void manyIncrementalRedactions() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final int records = 1000;
        final List<String> lines = new ArrayList<>();
        for(int i = 0; i < records; i++) {
            lines.add("Record " + i + " has SSN 123-45-" + String.format("%04d", 1000 + i) + " on file.");
        }
        final String input = String.join(" ", lines);

        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(properties), contextService, vectorService, null);
        final TextFilterResult response = service.filter(getPolicy(), "context", input);

        final List<IncrementalRedaction> redactions = response.getIncrementalRedactions();
        Assertions.assertEquals(records, redactions.size());

        // Each step replaces one more SSN, in order.
        for(int step = 0; step < records; step++) {
            final List<String> expected = new ArrayList<>(lines);
            for(int i = 0; i <= step; i++) {
                expected.set(i, lines.get(i).replaceFirst("123-45-\\d{4}", "{{{REDACTED-ssn}}}"));
            }
            final String text = String.join(" ", expected);
            Assertions.assertEquals(text, redactions.get(step).getIncrementallyRedactedText(), "step " + step);
            Assertions.assertEquals(DigestUtils.sha256Hex(text), redactions.get(step).getHash(), "step " + step);
        }

        Assertions.assertEquals(response.getFilteredText(), redactions.get(records - 1).getIncrementallyRedactedText());

    }

    @Test
    public void endToEndWithoutRedactionIncrements() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "George Washington was president.");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington was president.", response.getFilteredText());
        Assertions.assertTrue(response.getIncrementalRedactions().isEmpty(), "Expected no incremental redactions");

    }

    @Test
    public void endToEndWithSplits() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyWithSplits();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "George Washington whose SSN was 123-45-6789 was\n the first president of the United States and he lived at 90210.\nThe second president was John Adams. Abraham Lincoln was later on. His SSN was 123-45-6789.");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington whose SSN was {{{REDACTED-ssn}}} was\n the first president of the United States and he lived at {{{REDACTED-zip-code}}}.\nThe second president was John Adams. Abraham Lincoln was later on. His SSN was {{{REDACTED-ssn}}}.", response.getFilteredText());

        for(final IncrementalRedaction incrementalRedaction : response.getIncrementalRedactions()) {
            LOGGER.info("Incremental Redaction: {}", incrementalRedaction);
            Assertions.assertEquals(DigestUtils.sha256Hex(incrementalRedaction.getIncrementallyRedactedText()), incrementalRedaction.getHash());
        }

        final Gson gson = new Gson();
        LOGGER.info(gson.toJson(response));

    }

}
