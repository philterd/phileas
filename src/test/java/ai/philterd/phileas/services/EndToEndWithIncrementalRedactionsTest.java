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
import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.objects.IncrementalRedaction;
import ai.philterd.phileas.model.objects.FilterResponse;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.policy.Policy;
import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Properties;

import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicy;
import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyWithSplits;

public class EndToEndWithIncrementalRedactionsTest {

    private static final Logger LOGGER = LogManager.getLogger(EndToEndWithIncrementalRedactionsTest.class);

    private final ContextService contextService = Mockito.mock(ContextService.class);
    private final VectorService vectorService = Mockito.mock(VectorService.class);

    @Test
    public void endToEndWithRedactionIncrements() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context", "George Washington whose SSN was 123-45-6789 was the first president of the United States and he lived at 90210.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington whose SSN was {{{REDACTED-ssn}}} was the first president of the United States and he lived at {{{REDACTED-zip-code}}}.", response.getFilteredText());
        Assertions.assertTrue(CollectionUtils.isNotEmpty(response.getIncrementalRedactions()));

        for(final IncrementalRedaction incrementalRedaction : response.getIncrementalRedactions()) {
            LOGGER.info("Incremental Redaction: {}", incrementalRedaction);
            Assertions.assertEquals(DigestUtils.sha256Hex(incrementalRedaction.getIncrementallyRedactedText()), incrementalRedaction.getHash());
        }

    }

    @Test
    public void endToEndWithoutRedactionIncrements() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context", "George Washington was president.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington was president.", response.getFilteredText());
        Assertions.assertTrue(response.getIncrementalRedactions().isEmpty(), "Expected no incremental redactions");

    }

    @Test
    public void endToEndWithSplits() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyWithSplits("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context", "George Washington whose SSN was 123-45-6789 was\n the first president of the United States and he lived at 90210.\nThe second president was John Adams. Abraham Lincoln was later on. His SSN was 123-45-6789.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington whose SSN was {{{REDACTED-ssn}}} was\nthe first president of the United States and he lived at {{{REDACTED-zip-code}}}.\nThe second president was John Adams. Abraham Lincoln was later on. His SSN was {{{REDACTED-ssn}}}.", response.getFilteredText());

        for(final IncrementalRedaction incrementalRedaction : response.getIncrementalRedactions()) {
            LOGGER.info("Incremental Redaction: {}", incrementalRedaction);
            Assertions.assertEquals(DigestUtils.sha256Hex(incrementalRedaction.getIncrementallyRedactedText()), incrementalRedaction.getHash());
        }

        final Gson gson = new Gson();
        LOGGER.info(gson.toJson(response));

    }

}
