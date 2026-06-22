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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.CustomDictionary;
import ai.philterd.phileas.policy.filters.Identifier;
import ai.philterd.phileas.policy.filters.PhEye;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.policy.filters.ZipCode;
import ai.philterd.phileas.policy.filters.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.strategies.custom.CustomDictionaryFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.IdentifierFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FilterPolicyLoaderTest {

    private static final Logger LOGGER = LogManager.getLogger(FilterPolicyLoaderTest.class);

    @Test
    public void checkDefaultWindowSize() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        final Identifiers identifiers = new Identifiers();
        identifiers.setZipCode(new ZipCode());

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(1, filters.size());
        Assertions.assertEquals(5, filters.get(0).getWindowSize());

    }

    @Test
    public void checkCustomWindowSize() throws Exception {

        final Properties properties = new Properties();
        properties.put("span.window.size", "3");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);
        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        final Identifiers identifiers = new Identifiers();
        identifiers.setZipCode(new ZipCode());

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(1, filters.size());
        Assertions.assertEquals(3, filters.get(0).getWindowSize());

    }

    @Test
    public void getFiltersForPolicy() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        final Identifiers identifiers = new Identifiers();
        identifiers.setZipCode(new ZipCode());

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(1, filters.size());
        Assertions.assertEquals(1, filterCache.size());

    }

    @Test
    public void getFiltersForPolicyWithNoFilters() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        final Policy policy = new Policy();

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(0, filters.size());
        Assertions.assertEquals(1, filterCache.size());

    }

    @Test
    public void getFiltersForPolicyWithMultiplePhEye() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        final PhEye phEye1 = new PhEye();
        final PhEyeConfiguration config1 = new PhEyeConfiguration();
        config1.setEndpoint("http://localhost:5000");
        phEye1.setPhEyeConfiguration(config1);

        final PhEye phEye2 = new PhEye();
        final PhEyeConfiguration config2 = new PhEyeConfiguration();
        config2.setEndpoint("http://localhost:5001");
        phEye2.setPhEyeConfiguration(config2);

        final Identifiers identifiers = new Identifiers();
        identifiers.setPhEyes(List.of(phEye1, phEye2));

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(2, filters.size());

    }

    @Test
    public void getFiltersForPolicyWithPerson() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        final PhEye person = new PhEye();
        final PhEyeConfiguration config = new PhEyeConfiguration();
        config.setEndpoint("http://localhost:5000");
        person.setPhEyeConfiguration(config);

        final Identifiers identifiers = new Identifiers();
        identifiers.setPerson(person);

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(1, filters.size());
        Assertions.assertEquals(FilterType.PERSON, filters.get(0).getFilterType());

    }

    @Test
    public void getFiltersForPolicyCachesCustomDictionaryFilter() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));
        customDictionary.setTerms(List.of("george", "samuel"));

        final Identifiers identifiers = new Identifiers();
        identifiers.setCustomDictionaries(List.of(customDictionary));

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> first = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
        final List<Filter> second = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(1, first.size());
        Assertions.assertEquals(1, filterCache.size());

        // The custom dictionary filter is cached, so the second call reuses the same filter instance
        // instead of rebuilding it (and re-reading any dictionary files).
        Assertions.assertSame(first.get(0), second.get(0));

    }

    @Test
    public void getFiltersForPolicyCachesIdentifierFilter() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        final Identifier identifier = new Identifier();
        identifier.setIdentifierFilterStrategies(List.of(new IdentifierFilterStrategy()));
        identifier.setPattern("[0-9]{3}");

        final Identifiers identifiers = new Identifiers();
        identifiers.setIdentifiers(List.of(identifier));

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> first = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
        final List<Filter> second = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(1, first.size());
        Assertions.assertEquals(1, filterCache.size());

        // The identifier filter is cached, so the second call reuses the same filter instance.
        Assertions.assertSame(first.get(0), second.get(0));

    }

    @Test
    public void getFiltersForPolicyReusesCachedListAcrossCalls() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        final Identifiers identifiers = new Identifiers();
        identifiers.setZipCode(new ZipCode());

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> first = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
        final List<Filter> second = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        // The same policy returns the cached filter list instance on subsequent calls.
        Assertions.assertSame(first, second);
        Assertions.assertEquals(1, filterCache.size());

        // A different policy is cached under its own key.
        final Identifiers otherIdentifiers = new Identifiers();
        otherIdentifiers.setSsn(new Ssn());

        final Policy otherPolicy = new Policy();
        otherPolicy.setIdentifiers(otherIdentifiers);

        filterPolicyLoader.getFiltersForPolicy(otherPolicy, filterCache);

        Assertions.assertEquals(2, filterCache.size());

    }

    @Test
    public void getFiltersForPolicyCachesMultipleCustomDictionaries() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        final CustomDictionary first = new CustomDictionary();
        first.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));
        first.setTerms(List.of("george"));

        final CustomDictionary second = new CustomDictionary();
        second.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));
        second.setTerms(List.of("samuel"));

        final Identifiers identifiers = new Identifiers();
        identifiers.setCustomDictionaries(List.of(first, second));

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> firstCall = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);
        final List<Filter> secondCall = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        // Both dictionaries in the list are built and both are reused from the cache on the next call.
        Assertions.assertEquals(2, firstCall.size());
        Assertions.assertEquals(1, filterCache.size());
        Assertions.assertSame(firstCall.get(0), secondCall.get(0));
        Assertions.assertSame(firstCall.get(1), secondCall.get(1));

    }

    @Test
    public void getFiltersForPolicyExcludesDisabledCustomDictionary() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        final CustomDictionary enabled = new CustomDictionary();
        enabled.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));
        enabled.setTerms(List.of("george"));

        final CustomDictionary disabled = new CustomDictionary();
        disabled.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));
        disabled.setTerms(List.of("samuel"));
        disabled.setEnabled(false);

        final Identifiers identifiers = new Identifiers();
        identifiers.setCustomDictionaries(List.of(enabled, disabled));

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        // Only the enabled dictionary becomes a filter; the disabled one is excluded.
        Assertions.assertEquals(1, filters.size());
        Assertions.assertEquals(1, filterCache.size());

    }

    @Test
    public void getFiltersForPolicyExcludesCustomDictionaryWithNoTerms() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        // A custom dictionary with no terms (and no files) is not turned into a filter.
        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));

        final Identifiers identifiers = new Identifiers();
        identifiers.setCustomDictionaries(List.of(customDictionary));

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        Assertions.assertEquals(0, filters.size());
        Assertions.assertEquals(1, filterCache.size());

    }

    @Test
    public void getFiltersForPolicyExcludesDisabledIdentifier() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

        final FilterPolicyLoader filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, new SecureRandom(), null);

        final Identifier identifier = new Identifier();
        identifier.setIdentifierFilterStrategies(List.of(new IdentifierFilterStrategy()));
        identifier.setPattern("[0-9]{3}");
        identifier.setEnabled(false);

        final Identifiers identifiers = new Identifiers();
        identifiers.setIdentifiers(List.of(identifier));

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final Map<String, List<Filter>> filterCache = new HashMap<>();

        final List<Filter> filters = filterPolicyLoader.getFiltersForPolicy(policy, filterCache);

        // The disabled identifier is excluded from the filter list.
        Assertions.assertEquals(0, filters.size());
        Assertions.assertEquals(1, filterCache.size());

    }

}