package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicy;

/**
 * Verifies the prepared-policy API (issue #380): a prepared policy produces the same result as the
 * direct filter() call and can be reused across calls.
 */
public class PreparedPolicyTest {

    private PlainTextFilterService service() {
        return new PlainTextFilterService(
                new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
    }

    @Test
    public void preparedMatchesDirectFilter() throws Exception {
        final PlainTextFilterService service = service();
        final Policy policy = getPolicy();
        final String input = "My email is test@something.com and cc is 4121742025464465";

        final TextFilterResult direct = service.filter(policy, "context", input);
        final TextFilterResult prepared = service.prepare(policy).filter("context", input);

        Assertions.assertEquals(direct.getFilteredText(), prepared.getFilteredText());
    }

    @Test
    public void preparedPolicyIsReusable() throws Exception {
        final PlainTextFilterService.PreparedPolicy prepared = service().prepare(getPolicy());
        final String input = "My email is test@something.com and cc is 4121742025464465";
        final String expected = "My email is {{{REDACTED-email-address}}} and cc is {{{REDACTED-credit-card}}}";

        // Reusing the handle across calls produces the same result each time.
        Assertions.assertEquals(expected, prepared.filter("context", input).getFilteredText());
        Assertions.assertEquals(expected, prepared.filter("context", input).getFilteredText());
    }

}
