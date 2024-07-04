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
package ai.philterd.phileas.services.policies.cache;

import ai.philterd.phileas.model.services.PolicyCacheService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryPolicyCacheService implements PolicyCacheService {

    private Map<String, String> cache;

    public InMemoryPolicyCacheService() {
        this.cache = new HashMap<>();
    }

    @Override
    public List<String> get() throws IOException {
        return new ArrayList<>(cache.keySet());
    }

    @Override
    public String get(String policyName) throws IOException {
        return cache.get(policyName);
    }

    @Override
    public Map<String, String> getAll() throws IOException {
        return cache;
    }

    @Override
    public void insert(String policyName, String policy) {
        cache.put(policyName, policy);
    }

    @Override
    public void remove(String policyName) {
        cache.remove(policyName);
    }

    @Override
    public void clear() throws IOException {
        cache.clear();
    }

}
