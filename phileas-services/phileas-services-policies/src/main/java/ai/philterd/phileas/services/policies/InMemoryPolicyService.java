/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.exceptions.api.BadRequestException;
import ai.philterd.phileas.model.services.PolicyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryPolicyService implements PolicyService {

    private static final Logger LOGGER = LogManager.getLogger(InMemoryPolicyService.class);

    private final Map<String, String> policies;

    public InMemoryPolicyService() {
        this.policies = new HashMap<>();
    }

    @Override
    public List<String> get() throws IOException {

        return policies.keySet().stream().toList();

    }

    @Override
    public String get(String policyName) throws IOException {

        return policies.get(policyName);

    }

    @Override
    public Map<String, String> getAll() throws IOException {

        return policies;

    }

    @Override
    public void save(String policyJson) {

        try {

            final JSONObject object = new JSONObject(policyJson);
            final String policyName = object.getString("name");

            policies.put(policyName, policyJson);

        } catch (JSONException ex) {

            LOGGER.error("The provided policy is not valid.", ex);
            throw new BadRequestException("The provided policy is not valid.");

        }

    }

    @Override
    public void delete(String policyName) {

        policies.remove(policyName);

    }

}


