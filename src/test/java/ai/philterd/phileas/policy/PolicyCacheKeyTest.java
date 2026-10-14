package ai.philterd.phileas.policy;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * Verifies the memoized policy cache key (issue #379): the key is hashed once and reused across
 * calls, and distinct policies (the way a changed policy is represented) get distinct keys.
 */
public class PolicyCacheKeyTest {

    @Test
    public void keyIsMemoized() {
        final Policy policy = new Policy();
        // Same String instance on repeated calls means it was hashed once and reused, not
        // re-serialized and re-hashed per call (the per-row overhead this avoids).
        Assertions.assertSame(policy.getCacheKey(), policy.getCacheKey());
    }

    @Test
    public void differentContentProducesDifferentKey() {
        final Policy a = new Policy();
        final Policy b = new Policy();
        b.setIgnored(List.of(new Ignored()));
        Assertions.assertNotEquals(a.getCacheKey(), b.getCacheKey());
    }

    @Test
    public void sameContentProducesSameKey() {
        Assertions.assertEquals(new Policy().getCacheKey(), new Policy().getCacheKey());
    }

    @Test
    public void keyIsTheSha256OfThePolicyJson() throws Exception {
        // A filter cache can hold policies from different authors, so the key must be one that cannot be
        // made to collide: a policy given another's key would be filtered with the other's filters.
        final Policy policy = new Policy();
        policy.setIgnored(List.of(new Ignored()));
        final String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(new Gson().toJson(policy).getBytes(StandardCharsets.UTF_8)));
        Assertions.assertEquals(expected, policy.getCacheKey());
        Assertions.assertEquals(64, policy.getCacheKey().length());
    }

}
