/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface PolicyService {

    /**
     * Gets the names of all policies from
     * the backend store directly by-passing the cache.
     * @return A list of policy names.
     * @throws IOException
     */
    List<String> get() throws IOException;

    /**
     * Gets the content of a policy.
     * @param policyName
     * @return The policy.
     * @throws IOException
     */
    String get(String policyName) throws IOException;

    /**
     * Get the names and content of all policies.
     * @return A map of policy names to policy content.
     * @throws IOException
     */
    Map<String, String> getAll() throws IOException;

    /**
     * Saves a policy.
     * @param policyJson The content of the policy as JSON.
     * @throws IOException
     */
    void save(String policyJson) throws IOException;

    /**
     * Deletes a policy.
     * @param policyName The name of the policy to delete.
     * @throws IOException
     */
    void delete(String policyName) throws IOException;

}
