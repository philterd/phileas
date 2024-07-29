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
package ai.philterd.test.phileas.services;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.Filter;
import ai.philterd.phileas.model.policy.Identifiers;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.filters.ZipCode;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.model.services.AnonymizationCacheService;
import ai.philterd.phileas.model.services.MetricsService;
import ai.philterd.phileas.services.FilterPolicyLoader;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FilterPolicyLoaderTest {

    private static final Logger LOGGER = LogManager.getLogger(FilterPolicyLoaderTest.class);

    @Test
    public void getFiltersForPolicy() throws Exception {

        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);
        final AlertService alertService = Mockito.mock(AlertService.class);
        final MetricsService metricsService = Mockito.mock(MetricsService.class);
        final Map<String, DescriptiveStatistics> stats = new HashMap<>();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(alertService, anonymizationCacheService, metricsService, stats, phileasConfiguration);

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

        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);
        final AlertService alertService = Mockito.mock(AlertService.class);
        final MetricsService metricsService = Mockito.mock(MetricsService.class);
        final Map<String, DescriptiveStatistics> stats = new HashMap<>();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(alertService, anonymizationCacheService, metricsService, stats, phileasConfiguration);

        final Policy policy = new Policy();
        policy.setName("unnamed");

        final Map<String, Map<FilterType, Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(0, filters.size());
        Assertions.assertEquals(1, filterCache.size());
        Assertions.assertEquals(0, filterCache.get("unnamed").size());

    }

}