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
package ai.philterd.phileas.model.objects;

public class GetPolicyResult {

    private final String policyJson;
    private final boolean requiresReload;

    public GetPolicyResult(final String policyJson, final boolean requiresReload) {

        this.policyJson = policyJson;
        this.requiresReload = requiresReload;

    }

    public String getPolicyJson() {
        return policyJson;
    }

    public boolean isRequiresReload() {
        return requiresReload;
    }

}
