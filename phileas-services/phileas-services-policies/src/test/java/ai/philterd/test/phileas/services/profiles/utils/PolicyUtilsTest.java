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
package ai.philterd.test.phileas.services.policies.utils;

import com.google.gson.Gson;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.services.PolicyService;
import ai.philterd.phileas.services.policies.utils.PolicyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.mockito.Mockito.when;

public class PolicyUtilsTest {

    @Disabled
    @Test
    public void onlyOne() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy1.json"), Charset.defaultCharset());

        final PolicyService policyService = Mockito.mock(PolicyService.class);
        when(policyService.get("policy1")).thenReturn(json1);

        final Gson gson = new Gson();
        final PolicyUtils policyUtils = new PolicyUtils(policyService, gson);
        final Policy policy = policyUtils.getCombinedPolicies(Arrays.asList("policy1"));

        final Policy originalPolicy = gson.fromJson(json1, Policy.class);

        // TODO: This needs a deep comparison.
        Assertions.assertTrue(originalPolicy.equals(policy));

        Assertions.assertNotNull(policy);
        Assertions.assertFalse(StringUtils.equals(policy.getName(), "combined"));
        Assertions.assertTrue(policy.getIdentifiers().hasFilter(FilterType.AGE));
        Assertions.assertFalse(policy.getIdentifiers().hasFilter(FilterType.CREDIT_CARD));
        Assertions.assertFalse(policy.getIdentifiers().hasFilter(FilterType.URL));

    }

    @Test
    public void combineAgeAndCreditCard() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy1.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy2.json"), Charset.defaultCharset());

        final PolicyService policyService = Mockito.mock(PolicyService.class);
        when(policyService.get("policy1")).thenReturn(json1);
        when(policyService.get("policy2")).thenReturn(json2);

        final Gson gson = new Gson();
        final PolicyUtils policyUtils = new PolicyUtils(policyService, gson);
        final Policy policy = policyUtils.getCombinedPolicies(Arrays.asList("policy1", "policy2"));

        Assertions.assertNotNull(policy);
        Assertions.assertTrue(StringUtils.equals(policy.getName(), "combined"));
        Assertions.assertTrue(policy.getIdentifiers().hasFilter(FilterType.AGE));
        Assertions.assertTrue(policy.getIdentifiers().hasFilter(FilterType.CREDIT_CARD));
        Assertions.assertFalse(policy.getIdentifiers().hasFilter(FilterType.URL));

    }

    @Test
    public void combineDuplicateFilter() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy1.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy1.json"), Charset.defaultCharset());

        final PolicyService policyService = Mockito.mock(PolicyService.class);
        when(policyService.get("policy1")).thenReturn(json1);
        when(policyService.get("policy2")).thenReturn(json2);

        final Gson gson = new Gson();
        final PolicyUtils policyUtils = new PolicyUtils(policyService, gson);

        Assertions.assertThrows(IllegalStateException.class, () -> {
            final Policy policy = policyUtils.getCombinedPolicies(Arrays.asList("policy1", "policy2"));
        });

    }

    @Test
    public void combineCustomDictionaryAndZipCode() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy3.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy4.json"), Charset.defaultCharset());

        final PolicyService policyService = Mockito.mock(PolicyService.class);
        when(policyService.get("policy3")).thenReturn(json1);
        when(policyService.get("policy4")).thenReturn(json2);

        final Gson gson = new Gson();
        final PolicyUtils policyUtils = new PolicyUtils(policyService, gson);
        final Policy policy = policyUtils.getCombinedPolicies(Arrays.asList("policy3", "policy4"));

        Assertions.assertNotNull(policy);
        Assertions.assertTrue(policy.getIdentifiers().hasFilter(FilterType.CUSTOM_DICTIONARY));
        Assertions.assertTrue(policy.getIdentifiers().hasFilter(FilterType.ZIP_CODE));

    }

    @Test
    public void combineWithCryptoInFirst() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy5.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy6.json"), Charset.defaultCharset());

        final PolicyService policyService = Mockito.mock(PolicyService.class);
        when(policyService.get("policy5")).thenReturn(json1);
        when(policyService.get("policy6")).thenReturn(json2);

        final Gson gson = new Gson();
        final PolicyUtils policyUtils = new PolicyUtils(policyService, gson);
        final Policy policy = policyUtils.getCombinedPolicies(Arrays.asList("policy5", "policy6"));

        Assertions.assertNotNull(policy);
        Assertions.assertNotNull(policy.getCrypto());
        Assertions.assertTrue(StringUtils.equalsIgnoreCase(policy.getCrypto().getKey(), "keyhere"));
        Assertions.assertTrue(StringUtils.equalsIgnoreCase(policy.getCrypto().getIv(), "ivhere"));
        Assertions.assertTrue(policy.getIdentifiers().hasFilter(FilterType.ZIP_CODE));

    }

    @Test
    public void combineWithCryptoInSecond() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy6.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy5.json"), Charset.defaultCharset());

        final PolicyService policyService = Mockito.mock(PolicyService.class);
        when(policyService.get("policy6")).thenReturn(json1);
        when(policyService.get("policy5")).thenReturn(json2);

        final Gson gson = new Gson();
        final PolicyUtils policyUtils = new PolicyUtils(policyService, gson);
        final Policy policy = policyUtils.getCombinedPolicies(Arrays.asList("policy6", "policy5"));

        Assertions.assertNotNull(policy);
        Assertions.assertNull(policy.getCrypto());
        Assertions.assertTrue(policy.getIdentifiers().hasFilter(FilterType.ZIP_CODE));

    }

    @Test
    public void combineWithConfigInFirst() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy7.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy5.json"), Charset.defaultCharset());

        final PolicyService policyService = Mockito.mock(PolicyService.class);
        when(policyService.get("policy7")).thenReturn(json1);
        when(policyService.get("policy5")).thenReturn(json2);

        final Gson gson = new Gson();
        final PolicyUtils policyUtils = new PolicyUtils(policyService, gson);
        final Policy policy = policyUtils.getCombinedPolicies(Arrays.asList("policy7", "policy5"));

        Assertions.assertNotNull(policy);
        Assertions.assertNull(policy.getCrypto());
        Assertions.assertTrue(policy.getIdentifiers().hasFilter(FilterType.ZIP_CODE));

    }

    @Test
    public void combineWithIgnored() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy8.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy9.json"), Charset.defaultCharset());

        final PolicyService policyService = Mockito.mock(PolicyService.class);
        when(policyService.get("policy8")).thenReturn(json1);
        when(policyService.get("policy9")).thenReturn(json2);

        final Gson gson = new Gson();
        final PolicyUtils policyUtils = new PolicyUtils(policyService, gson);
        final Policy policy = policyUtils.getCombinedPolicies(Arrays.asList("policy8", "policy9"));

        Assertions.assertNotNull(policy);
        Assertions.assertNotNull(policy.getIgnored());
        Assertions.assertEquals(2, policy.getIgnored().size());
        Assertions.assertTrue(policy.getIdentifiers().hasFilter(FilterType.ZIP_CODE));

    }

    @Test
    public void combineWithIgnoredPatterns() throws IOException {

        final String json1 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy10.json"), Charset.defaultCharset());
        final String json2 = IOUtils.toString(this.getClass().getResourceAsStream("/policies/policy11.json"), Charset.defaultCharset());

        final PolicyService policyService = Mockito.mock(PolicyService.class);
        when(policyService.get("policy10")).thenReturn(json1);
        when(policyService.get("policy11")).thenReturn(json2);

        final Gson gson = new Gson();
        final PolicyUtils policyUtils = new PolicyUtils(policyService, gson);
        final Policy policy = policyUtils.getCombinedPolicies(Arrays.asList("policy10", "policy11"));

        Assertions.assertNotNull(policy);
        Assertions.assertNotNull(policy.getIgnored());
        Assertions.assertEquals(2, policy.getIgnoredPatterns().size());
        Assertions.assertFalse(policy.getIdentifiers().hasFilter(FilterType.ZIP_CODE));

    }

}
