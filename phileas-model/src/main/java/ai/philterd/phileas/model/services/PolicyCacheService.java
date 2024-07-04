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
package ai.philterd.phileas.model.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface PolicyCacheService {

    /**
     * Gets the names of all policies.
     * @return
     * @throws IOException
     */
    List<String> get() throws IOException;

    /**
     * Gets the content of a policy.
     * @param policyName
     * @return
     * @throws IOException
     */
    String get(String policyName) throws IOException;

    /**
     * Get the names and content of all policies.
     * @return
     * @throws IOException
     */
    Map<String, String> getAll() throws IOException;

    /**
     * Inserts a new policy into the cache.
     * @param policyName The name of the policy.
     * @param policy The content of the policy.
     */
    void insert(String policyName, String policy);

    /**
     * Removes a policy from the cache.
     * @param policyName The name of the policy.
     */
    void remove(String policyName);

    /**
     * Clears all policies from the cache.
     * @throws IOException
     */
    void clear() throws IOException;

}
