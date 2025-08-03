package ai.philterd.test.phileas.services;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.objects.IncrementalRedaction;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.responses.FilterResponse;
import ai.philterd.phileas.model.services.CacheService;
import ai.philterd.phileas.services.PhileasFilterService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Properties;

import static ai.philterd.test.phileas.services.EndToEndTestsHelper.getPolicy;
import static ai.philterd.test.phileas.services.EndToEndTestsHelper.getPolicyWithSplits;

public class EndToEndWithIncrementalRedactionsTest {

    private static final Logger LOGGER = LogManager.getLogger(EndToEndWithIncrementalRedactionsTest.class);

    private final CacheService cacheService = Mockito.mock(CacheService.class);

    @Test
    public void endToEndWithRedactionIncrements() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, cacheService);
        final FilterResponse response = service.filter(policy, "context", "documentid", "George Washington whose SSN was 123-45-6789 was the first president of the United States and he lived at 90210.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington whose SSN was {{{REDACTED-ssn}}} was the first president of the United States and he lived at {{{REDACTED-zip-code}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());
        Assertions.assertTrue(CollectionUtils.isNotEmpty(response.getIncrementalRedactions()));

        for(final IncrementalRedaction incrementalRedaction : response.getIncrementalRedactions()) {
            LOGGER.info("Incremental Redaction: {}", incrementalRedaction.toString());
            Assertions.assertEquals(DigestUtils.sha256Hex(incrementalRedaction.getIncrementallyRedactedText()), incrementalRedaction.getHash());
        }

    }

    @Test
    public void endToEndWithoutRedactionIncrements() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, cacheService);
        final FilterResponse response = service.filter(policy, "context", "documentid", "George Washington was president.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington was president.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());
        Assertions.assertTrue(response.getIncrementalRedactions().isEmpty(), "Expected no incremental redactions");

    }

    @Test
    public void endToEndWithSplits() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyWithSplits("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, cacheService);
        final FilterResponse response = service.filter(policy, "context", "documentid", "George Washington whose SSN was 123-45-6789 was\n the first president of the United States and he lived at 90210.\nThe second president was John Adams. Abraham Lincoln was later on. His SSN was 123-45-6789.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington whose SSN was {{{REDACTED-ssn}}} was\nthe first president of the United States and he lived at {{{REDACTED-zip-code}}}.\nThe second president was John Adams. Abraham Lincoln was later on. His SSN was {{{REDACTED-ssn}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

        for(final IncrementalRedaction incrementalRedaction : response.getIncrementalRedactions()) {
            LOGGER.info("Incremental Redaction: {}", incrementalRedaction.toString());
            Assertions.assertEquals(DigestUtils.sha256Hex(incrementalRedaction.getIncrementallyRedactedText()), incrementalRedaction.getHash());
        }

    }


}
