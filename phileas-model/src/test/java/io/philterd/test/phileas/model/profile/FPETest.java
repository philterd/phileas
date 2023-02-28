package io.philterd.test.phileas.model.profile;

import io.philterd.phileas.model.profile.FPE;
import org.junit.jupiter.api.Test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FPETest {

    @Test
    public void test1() {

        final FPE crypto = new FPE("mykey", "myiv");

        assertEquals("mykey", crypto.getKey());
        assertEquals("myiv", crypto.getTweak());

    }

    @Test
    public void test2() throws Exception {

        final FPE crypto = new FPE("env:mykey", "myiv");

        final String value = withEnvironmentVariable("mykey", "value").execute(() -> crypto.getKey());

        assertEquals("value", value);
        assertEquals("myiv", crypto.getTweak());

    }

    @Test
    public void test3() throws Exception {

        final FPE crypto = new FPE("mykey", "env:myiv");

        final String value = withEnvironmentVariable("myiv", "value").execute(() -> crypto.getTweak());

        assertEquals("mykey", crypto.getKey());
        assertEquals("value", value);

    }

}
