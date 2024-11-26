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
package ai.philterd.phileas.services.policies;

import com.google.gson.Gson;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.services.PolicyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link PolicyService} that uses a static
 * policy
 */
public class StaticPolicyService implements PolicyService {

    private static final Logger LOGGER = LogManager.getLogger(StaticPolicyService.class);

    private final Policy policy;
    private Gson gson;

    /**
     * Creates a static filter.
     * @param policyJson The policy JSON string.
     */
    public StaticPolicyService(String policyJson) {

        this.gson = new Gson();
        this.policy = gson.fromJson(policyJson, Policy.class);

    }

    /**
     * Creates a static filter.
     * @param policy A {@link Policy}.
     */
    public StaticPolicyService(Policy policy) {

        this.policy = policy;

    }

    @Override
    public List<String> get() throws IOException {
        return Collections.emptyList();
    }

    /**
     * Gets the policy. Note that the <code>policyName</code> argument
     * is ignored since there is only a single policy.
     * @param policyName The name of the policy. This value is ignored.
     * @return The policy.
     */
    @Override
    public String get(String policyName) {

        // The policyName does not matter.
        // There is only one policy and it is returned.
        LOGGER.debug("Retrieving policy {}", policy);

        return gson.toJson(policy);

    }

    /**
     * Gets a map of policies. Note that the map will always only contain
     * a single policy.
     * @return A map of policies.
     */
    @Override
    public Map<String, String> getAll() {

        final Map<String, String> policies = new HashMap<>();

        policies.put(policy.getName(), gson.toJson(policy));

        return policies;

    }

    @Override
    public void save(String policyJson) throws IOException {
        // Will not be implemented.
    }

    @Override
    public void delete(String policyName) throws IOException {
        // Will not be implemented.
    }

}
