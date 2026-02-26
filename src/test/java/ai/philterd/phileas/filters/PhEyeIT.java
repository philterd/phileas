package ai.philterd.phileas.filters;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.PhEye;
import ai.philterd.phileas.policy.filters.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.ai.PhEyeFilterStrategy;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

@Disabled("Disabled until this is an integration tests and there can be a ph-eye service running to test against.")
public class PhEyeIT {

    private static final Logger LOGGER = LogManager.getLogger(PhEyeIT.class);

    private ContextService contextService;
    private PhileasConfiguration phileasConfiguration;
    private HttpClient httpClient;

    @BeforeEach
    public void before() {
        contextService = new DefaultContextService();
        phileasConfiguration = new PhileasConfiguration(new Properties());
        httpClient = HttpClients.createDefault();
    }

    @Test
    public void testPhEyePolicy() throws Exception {

        final PhEye phEye = new PhEye();
        phEye.setPhEyeFilterStrategies(List.of(new PhEyeFilterStrategy()));

        final PhEyeConfiguration config = new PhEyeConfiguration();
        config.setEndpoint("http://localhost:32784");
        phEye.setPhEyeConfiguration(config);

        final Identifiers identifiers = new Identifiers();
        identifiers.setPhEyes(List.of(phEye));

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, null, httpClient);
        final TextFilterResult response = service.filter(policy, "context", "George Washington was the first president.");

        LOGGER.info(response.getFilteredText());

        Assertions.assertNotNull(response);
        Assertions.assertEquals("{{{REDACTED-person}}} was the first president.", response.getFilteredText());

    }

    @Test
    public void testMultiplePhEyeFilters() throws Exception {

        // First PhEye filter for PERSON
        final PhEye phEye1 = new PhEye();
        phEye1.setPhEyeFilterStrategies(List.of(new PhEyeFilterStrategy()));
        final PhEyeConfiguration config1 = new PhEyeConfiguration();
        config1.setEndpoint("http://localhost:32784");
        config1.setLabels(List.of("PERSON"));
        phEye1.setPhEyeConfiguration(config1);

        // Second PhEye filter for HOSPITAL
        final PhEye phEye2 = new PhEye();
        phEye2.setPhEyeFilterStrategies(List.of(new PhEyeFilterStrategy()));
        final PhEyeConfiguration config2 = new PhEyeConfiguration();
        config2.setEndpoint("http://localhost:32784");
        config2.setLabels(List.of("HOSPITAL"));
        phEye2.setPhEyeConfiguration(config2);

        final Identifiers identifiers = new Identifiers();
        identifiers.setPhEyes(List.of(phEye1, phEye2));

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, null, httpClient);
        final TextFilterResult response = service.filter(policy, "context", "George Washington was in Chicago General Hospital.");

        LOGGER.info(response.getFilteredText());

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.getFilteredText().contains("{{{REDACTED-person}}}"));
        Assertions.assertTrue(response.getFilteredText().contains("{{{REDACTED-hospital}}}"));

    }

}
