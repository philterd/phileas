/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.services.strategies.rules.CanadaSinFilterStrategy;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CanadaSin extends AbstractFilter {

    @SerializedName("canadaSinFilterStrategies")
    @Expose
    private List<CanadaSinFilterStrategy> canadaSinFilterStrategies;

    /**
     * When {@code true}, do not detect SINs beginning with 0 or 8, which are not issued to
     * individuals. Default {@code false} (match any Luhn-valid nine-digit value).
     */
    @SerializedName("onlyValidPrefixes")
    @Expose
    private boolean onlyValidPrefixes = false;

    public List<CanadaSinFilterStrategy> getCanadaSinFilterStrategies() {
        return canadaSinFilterStrategies;
    }

    public void setCanadaSinFilterStrategies(List<CanadaSinFilterStrategy> canadaSinFilterStrategies) {
        this.canadaSinFilterStrategies = canadaSinFilterStrategies;
    }

    public boolean isOnlyValidPrefixes() {
        return onlyValidPrefixes;
    }

    public void setOnlyValidPrefixes(boolean onlyValidPrefixes) {
        this.onlyValidPrefixes = onlyValidPrefixes;
    }

}
