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
package ai.philterd.test.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.model.services.ContextService;
import ai.philterd.phileas.model.services.DefaultContextService;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.ZipCode;
import ai.philterd.phileas.services.FilterPolicyLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FilterPolicyLoaderTest {

    private static final Logger LOGGER = LogManager.getLogger(FilterPolicyLoaderTest.class);

    @Test
    public void checkDefaultWindowSize() throws Exception {

        final ContextService contextService = new DefaultContextService();

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());
        final Map<String, String> context = Collections.emptyMap();

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(contextService, phileasConfiguration);

        final Identifiers identifiers = new Identifiers();
        identifiers.setZipCode(new ZipCode());

        final Policy policy = new Policy();
        policy.setName("unnamed");
        policy.setIdentifiers(identifiers);

        final Map<String, Map<FilterType, Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(1, filters.size());
        Assertions.assertEquals(5, filters.get(0).getWindowSize());

    }

    @Test
    public void checkCustomWindowSize() throws Exception {

        final ContextService contextService = new DefaultContextService();

        final Properties properties = new Properties();
        properties.put("span.window.size", "3");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);
        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(contextService, phileasConfiguration);
        final Map<String, String> context = Collections.emptyMap();

        final Identifiers identifiers = new Identifiers();
        identifiers.setZipCode(new ZipCode());

        final Policy policy = new Policy();
        policy.setName("unnamed");
        policy.setIdentifiers(identifiers);

        final Map<String, Map<FilterType, Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(1, filters.size());
        Assertions.assertEquals(3, filters.get(0).getWindowSize());

    }

    @Test
    public void getFiltersForPolicy() throws Exception {

        final ContextService contextService = new DefaultContextService();

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());
        final Map<String, String> context = Collections.emptyMap();

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(contextService, phileasConfiguration);

        final Identifiers identifiers = new Identifiers();
        identifiers.setZipCode(new ZipCode());

        final Policy policy = new Policy();
        policy.setName("unnamed");
        policy.setIdentifiers(identifiers);

        final Map<String, Map<FilterType, Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(1, filters.size());
        Assertions.assertEquals(1, filterCache.size());
        Assertions.assertNotNull(filterCache.get("unnamed").get(FilterType.ZIP_CODE));

    }

    @Test
    public void getFiltersForPolicyWithNoFilters() throws Exception {

        final ContextService contextService = new DefaultContextService();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());
        final Map<String, String> context = Collections.emptyMap();

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(contextService, phileasConfiguration);

        final Policy policy = new Policy();
        policy.setName("unnamed");

        final Map<String, Map<FilterType, Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(0, filters.size());
        Assertions.assertEquals(1, filterCache.size());
        Assertions.assertEquals(0, filterCache.get("unnamed").size());

    }

}