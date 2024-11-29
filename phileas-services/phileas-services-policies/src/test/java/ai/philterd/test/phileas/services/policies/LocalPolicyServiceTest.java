/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.test.phileas.services.policies;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.policy.Identifiers;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.filters.Age;
import ai.philterd.phileas.model.policy.filters.CreditCard;
import ai.philterd.phileas.model.policy.filters.strategies.rules.AgeFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.CreditCardFilterStrategy;
import ai.philterd.phileas.model.services.PolicyService;
import ai.philterd.phileas.services.policies.LocalPolicyService;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class LocalPolicyServiceTest {

    private final Gson gson = new Gson();

    private PhileasConfiguration getConfiguration() throws IOException {

        final String tempDirectory = Files.createTempDirectory("phileas-policies").toFile().getAbsolutePath();

        final Properties properties = new Properties();
        properties.setProperty("filter.policies.directory", tempDirectory);
        properties.setProperty("cache.redis.enabled", "false");

        return new PhileasConfiguration(properties);

    }

    @Test
    public void list() throws IOException {

        final PolicyService policyService = new LocalPolicyService(getConfiguration());

        policyService.save(gson.toJson(getPolicy("name1")));
        policyService.save(gson.toJson(getPolicy("name2")));

        final List<String> names = policyService.get();

        Assertions.assertEquals(2, names.size());
        Assertions.assertTrue(names.contains("name1"));
        Assertions.assertTrue(names.contains("name2"));

    }

    @Test
    public void getAll() throws IOException {

        final PolicyService policyService = new LocalPolicyService(getConfiguration());

        policyService.save(gson.toJson(getPolicy("name1")));
        policyService.save(gson.toJson(getPolicy("name2")));

        final Map<String, String> all = policyService.getAll();

        Assertions.assertEquals(2, all.size());
        Assertions.assertTrue(all.containsKey("name1"));
        Assertions.assertTrue(all.containsKey("name2"));

    }

    @Test
    public void save() throws IOException {

        final String name = "default";

        final String policy = gson.toJson(getPolicy(name));

        final PolicyService policyService = new LocalPolicyService(getConfiguration());

        policyService.save(policy);

        final String saved = policyService.get("default");

        Assertions.assertNotNull(saved);
        Assertions.assertEquals(policy, saved);

    }

    @Test
    public void get() throws IOException {

        final String name = "default";

        final String policy = gson.toJson(getPolicy(name));

        final PolicyService policyService = new LocalPolicyService(getConfiguration());

        policyService.save(policy);

        final String policyJson = policyService.get(name);

        Assertions.assertEquals(policy, policyJson);

    }

    @Test
    public void delete() throws IOException {

        final String name = "default";
        final String policy = gson.toJson(getPolicy(name));

        final PolicyService policyService = new LocalPolicyService(getConfiguration());

        policyService.save(policy);

        policyService.delete(name);

        Assertions.assertFalse(policyService.getAll().containsKey(name));

    }

    @Test
    public void deleteOutsidePath() throws IOException {

        final File tempFile = File.createTempFile("phileas-", "-temp");
        tempFile.deleteOnExit();

        Assertions.assertTrue(Files.exists(tempFile.toPath()));

        final String name = "../" + tempFile.getName();

        final PolicyService policyService = new LocalPolicyService(getConfiguration());

        Assertions.assertThrows(IOException.class, () -> policyService.delete(name));

    }

    private Policy getPolicy(String name) {

        AgeFilterStrategy ageFilterStrategy = new AgeFilterStrategy();

        Age age = new Age();
        age.setAgeFilterStrategies(List.of(ageFilterStrategy));

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(List.of(creditCardFilterStrategy));

        Identifiers identifiers = new Identifiers();

        identifiers.setAge(age);

        Policy policy = new Policy();
        policy.setName(name);
        policy.setIdentifiers(identifiers);

        return policy;

    }

}
