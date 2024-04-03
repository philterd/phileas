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
package ai.philterd.phileas.services.policies.cache;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.cache.AbstractRedisCacheService;
import ai.philterd.phileas.model.services.PolicyCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedisPolicyCacheService extends AbstractRedisCacheService implements PolicyCacheService {

    private static final Logger LOGGER = LogManager.getLogger(RedisPolicyCacheService.class);

    private static final String CACHE_KEY = "policies";
    
    public RedisPolicyCacheService(PhileasConfiguration phileasConfiguration) throws IOException {
        super(phileasConfiguration);

        LOGGER.info("Initializing Reds policy cache.");
    }

    @Override
    public List<String> get() {

        // Get the names from the cache and return them.
        final RMap<String, String> names = redisson.getMap(CACHE_KEY);

        return new ArrayList<>(names.keySet());

    }

    @Override
    public String get(String policyName) {

        // Get the policy from the cache and return it.
        final RMap<String, String> map = redisson.getMap(CACHE_KEY);

        return map.get(policyName);

    }

    @Override
    public Map<String, String> getAll() {

        final long count = redisson.getKeys().countExists(CACHE_KEY);

        if(count != 0) {

            // Get the policies from the cache and return them.
            final RMap<String, String> map = redisson.getMap(CACHE_KEY);

            Map<String, String> m = new HashMap<>();
            for(String k : map.keySet()) {
                m.put(k, map.get(k));
            }

            return m;

        } else {

            return new HashMap<>();

        }

    }

    @Override
    public void insert(String policyName, String policy) {

        // Insert into the the cache.
        final RMap<String, String> map = redisson.getMap(CACHE_KEY);
        map.put(policyName, policy);

    }

    @Override
    public void remove(String policyName) {

        // Remove from the cache.
        final RMap<String, String> map = redisson.getMap(CACHE_KEY);
        map.remove(policyName);

    }

    @Override
    public void clear() {

        // Clear the cache.
        final RMap<String, String> map = redisson.getMap(CACHE_KEY);
        map.clear();

    }

}