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
package ai.philterd.phileas.services.policies.utils;

import com.google.gson.Gson;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.services.PolicyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class PolicyUtils {

    private static final Logger LOGGER = LogManager.getLogger(PolicyUtils.class);

    private PolicyService policyService;
    private Gson gson;

    public PolicyUtils(PolicyService policyService, Gson gson) {

        this.policyService = policyService;
        this.gson = gson;

    }

    public Policy getCombinedPolicys(final List<String> policyNames) throws IOException, IllegalStateException {

        // Get the deserialized policy of the first policy in the list.
        // By starting off with a full policy we don't have to worry about adding
        // Config and Crypto (and other sections) since those will always be
        // taken from the first policy.
        final Policy combinedPolicy = getPolicy(policyNames.get(0));

        // In some chases there may be only one policy.
        if(policyNames.size() > 1) {

            // The name has no bearing on the results. We just want to give it a name.
            combinedPolicy.setName("combined");

            // Loop over the policy names and skip the first one since we have already
            // deserialized it to a policy a few lines above.
            for (final String policyName : policyNames.subList(1, policyNames.size())) {

                // Get the policy.
                final Policy policy = getPolicy(policyName);

                // For each of the filter types, copy the filter (if it exists) from the source policy
                // to the destination (combined) policy. If a filter already exists in the destination (combined)
                // policy then throw an error.

                for(FilterType filterType : FilterType.values()) {
                    if (policy.getIdentifiers().hasFilter(filterType)) {
                        if (!combinedPolicy.getIdentifiers().hasFilter(filterType)) {
                            combinedPolicy.getIdentifiers().setFilter(filterType, policy.getIdentifiers().getFilter(filterType));
                        } else {
                            throw new IllegalStateException("Policy has duplicate filter: " + filterType.toString());
                        }
                    }
                }

                // Aggregate the Ignored and IgnoredPatterns into the combined policy.
                combinedPolicy.getIgnored().addAll(policy.getIgnored());
                combinedPolicy.getIgnoredPatterns().addAll(policy.getIgnoredPatterns());

            }

            // The Config and Crypto sections are expected to be in the first policy.
            // The Config and Crypto sections in the other policies are ignored.

            // As other sections are added to the policy they will need added here, too.

        }

        return combinedPolicy;

    }

    private Policy getPolicy(String policyName) throws IOException {

        // This will ALWAYS return a policy because if it is not in the cache it will be retrieved from the cache.
        // TODO: How to trigger a reload if the policy had to be retrieved from disk?
        final String policyJson = policyService.get(policyName);

        LOGGER.debug("Deserializing policy [{}]", policyName);
        return gson.fromJson(policyJson, Policy.class);

    }

}
