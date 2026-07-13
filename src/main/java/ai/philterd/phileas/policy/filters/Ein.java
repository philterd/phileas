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
package ai.philterd.phileas.policy.filters;

import ai.philterd.phileas.services.strategies.rules.EinFilterStrategy;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Ein extends AbstractFilter {

    @SerializedName("einFilterStrategies")
    @Expose
    private List<EinFilterStrategy> einFilterStrategies;

    /**
     * When {@code true}, only detect EINs whose two-digit prefix is one the IRS currently issues.
     * Default {@code false} (match any EIN-formatted value).
     */
    @SerializedName("onlyValidPrefixes")
    @Expose
    private boolean onlyValidPrefixes = false;

    public List<EinFilterStrategy> getEinFilterStrategies() {
        return einFilterStrategies;
    }

    public void setEinFilterStrategies(List<EinFilterStrategy> einFilterStrategies) {
        this.einFilterStrategies = einFilterStrategies;
    }

    public boolean isOnlyValidPrefixes() {
        return onlyValidPrefixes;
    }

    public void setOnlyValidPrefixes(boolean onlyValidPrefixes) {
        this.onlyValidPrefixes = onlyValidPrefixes;
    }

}
