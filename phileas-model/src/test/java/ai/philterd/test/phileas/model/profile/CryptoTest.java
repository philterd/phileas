/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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
