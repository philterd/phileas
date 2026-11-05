/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.policy;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.services.FilterPolicyLoader;
import ai.philterd.phileas.services.PlaceholderDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class PolicyEnvironmentTest {

    private static final String HOME = System.getenv("HOME");

    @AfterEach
    public void reset() {
        PolicyEnvironment.unrestricted();
    }

    @Test
    public void unrestrictedResolvesAnyVariable() {
        Assertions.assertEquals(HOME, new Crypto("env:HOME", "env:HOME").getKey());
        Assertions.assertEquals(System.getenv("PATH"), new FPE("env:PATH", "env:PATH").getTweak());
    }

    @Test
    public void aListedVariableResolvesAndAnyOtherIsRefusedByEveryGetter() {
        PolicyEnvironment.restrictTo(List.of("HOME"));
        Assertions.assertEquals(HOME, new Crypto("env:HOME", "env:HOME").getKey());
        Assertions.assertEquals(HOME, new Crypto("env:HOME", "env:HOME").getIv());
        Assertions.assertEquals(HOME, new FPE("env:HOME", "env:HOME").getKey());
        Assertions.assertEquals(HOME, new FPE("env:HOME", "env:HOME").getTweak());

        Assertions.assertEquals("The crypto key names the environment variable PATH, which a policy may not use here.",
                Assertions.assertThrows(InvalidPolicyEnvironmentException.class, () -> new Crypto("env:PATH", null).getKey()).getMessage());
        Assertions.assertThrows(InvalidPolicyEnvironmentException.class, () -> new Crypto(null, "env:PATH").getIv());
        Assertions.assertThrows(InvalidPolicyEnvironmentException.class, () -> new FPE("env:PATH", null).getKey());
        Assertions.assertThrows(InvalidPolicyEnvironmentException.class, () -> new FPE(null, "env:PATH").getTweak());
    }

    @Test
    public void theMessageIsTheSameWhetherOrNotTheVariableExists() {
        PolicyEnvironment.restrictTo(List.of());
        final String existing = Assertions.assertThrows(InvalidPolicyEnvironmentException.class,
                () -> new Crypto("env:PATH", null).getKey()).getMessage();
        final String missing = Assertions.assertThrows(InvalidPolicyEnvironmentException.class,
                () -> new Crypto("env:PHILEAS_NO_SUCH_VARIABLE_446", null).getKey()).getMessage();
        Assertions.assertEquals(existing.replace("PATH", "X"), missing.replace("PHILEAS_NO_SUCH_VARIABLE_446", "X"));
    }

    @Test
    public void anEmptyAllowlistAllowsNoVariableAndLiteralsAreUnaffected() {
        PolicyEnvironment.restrictTo(List.of());
        Assertions.assertThrows(InvalidPolicyEnvironmentException.class, () -> new Crypto("env:HOME", null).getKey());
        Assertions.assertEquals("9EE7A356FDFE43F069500B0086758346", new Crypto("9EE7A356FDFE43F069500B0086758346", null).getKey());
        Assertions.assertNull(new FPE(null, null).getKey());
    }

    @Test
    public void aPolicyNamingAnUnlistedVariableIsRefusedWhenLoaded() {
        PolicyEnvironment.restrictTo(List.of("HOME"));
        final FilterPolicyLoader loader = new FilterPolicyLoader(new PhileasConfiguration(new Properties()), new SecureRandom(), null);
        for (final String keys : new String[]{"\"crypto\":{\"key\":\"env:PATH\",\"iv\":\"env:HOME\"}", "\"fpe\":{\"key\":\"env:HOME\",\"tweak\":\"env:PATH\"}"}) {
            final Policy policy = new Gson().fromJson("{" + keys + ",\"identifiers\":{\"ssn\":{}}}", Policy.class);
            Assertions.assertThrows(InvalidPolicyEnvironmentException.class, () -> loader.getFiltersForPolicy(policy, new HashMap<>()), keys);
        }
        final Policy allowed = new Gson().fromJson("{\"crypto\":{\"key\":\"env:HOME\",\"iv\":\"env:HOME\"},\"identifiers\":{\"ssn\":{}}}", Policy.class);
        Assertions.assertDoesNotThrow(() -> loader.getFiltersForPolicy(allowed, new HashMap<>()));
    }

    @Test
    public void thePlaceholderDeserializerResolvesOnlyListedVariables() {
        final Gson gson = new GsonBuilder().registerTypeAdapter(String.class, new PlaceholderDeserializer()).create();
        PolicyEnvironment.restrictTo(List.of("HOME"));
        Assertions.assertEquals(HOME, gson.fromJson("\"${HOME}\"", String.class));
        Assertions.assertEquals("${PATH}", gson.fromJson("\"${PATH}\"", String.class));
    }

}
