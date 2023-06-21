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

import ai.philterd.phileas.model.profile.FPE;
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
