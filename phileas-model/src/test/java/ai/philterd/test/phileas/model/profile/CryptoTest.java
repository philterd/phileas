package ai.philterd.test.phileas.model.profile;

import ai.philterd.phileas.model.profile.Crypto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CryptoTest {

    @Test
    public void test1() {

        final Crypto crypto = new Crypto("mykey", "myiv");

        assertEquals("mykey", crypto.getKey());
        assertEquals("myiv", crypto.getIv());

    }

    @Test
    public void test2() throws Exception {

        final Crypto crypto = new Crypto("env:mykey", "myiv");

        final String value = withEnvironmentVariable("mykey", "value").execute(() -> crypto.getKey());

        assertEquals("value", value);
        assertEquals("myiv", crypto.getIv());

    }

    @Test
    public void test3() throws Exception {

        final Crypto crypto = new Crypto("mykey", "env:myiv");

        final String value = withEnvironmentVariable("myiv", "value").execute(() -> crypto.getIv());

        assertEquals("mykey", crypto.getKey());
        assertEquals("value", value);

    }

}
