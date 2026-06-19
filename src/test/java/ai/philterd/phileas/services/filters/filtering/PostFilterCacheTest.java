package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicy;

/**
 * Verifies post-filters are cached per policy (issue #382): a repeated policy reuses one post-filter
 * list instead of rebuilding it on every call, and distinct policies get distinct lists.
 */
public class PostFilterCacheTest {

    private PlainTextFilterService service() {
        return new PlainTextFilterService(
                new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
    }

    @Test
    public void postFiltersAreCachedPerPolicy() throws Exception {
        final PlainTextFilterService service = service();
        final Policy policy = getPolicy();

        // Same instance on repeated calls means the list was built once and reused, not rebuilt per call.
        Assertions.assertSame(service.getPostFiltersForPolicy(policy), service.getPostFiltersForPolicy(policy));
    }

    @Test
    public void differentPoliciesGetDifferentPostFilters() throws Exception {
        final PlainTextFilterService service = service();
        Assertions.assertNotSame(
                service.getPostFiltersForPolicy(getPolicy()),
                service.getPostFiltersForPolicy(new Policy()));
    }

    @Test
    public void repeatedFilterCallsCachePostFiltersOnce() throws Exception {
        final PlainTextFilterService service = service();
        final Policy policy = getPolicy();
        final String input = "My email is test@something.com and cc is 4121742025464465";

        // Two full filter() calls with the same policy leave a single cached post-filter list.
        service.filter(policy, "context", input);
        service.filter(policy, "context", input);

        Assertions.assertEquals(1, service.postFilterCache.size());
    }

}
