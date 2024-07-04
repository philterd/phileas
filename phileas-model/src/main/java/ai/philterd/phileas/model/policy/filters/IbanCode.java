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
package ai.philterd.phileas.model.policy.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.policy.filters.strategies.rules.IbanCodeFilterStrategy;

import java.util.List;

public class IbanCode extends AbstractFilter {

    @SerializedName("onlyValidIBANCodes")
    @Expose
    protected boolean onlyValidIBANCodes = true;

    @SerializedName("allowSpaces")
    @Expose
    protected boolean allowSpaces = true;

    @SerializedName("ibanCodeFilterStrategies")
    @Expose
    private List<IbanCodeFilterStrategy> ibanCodeFilterStrategies;

    public List<IbanCodeFilterStrategy> getIbanCodeFilterStrategies() {
        return ibanCodeFilterStrategies;
    }

    public void setIbanCodeFilterStrategies(List<IbanCodeFilterStrategy> ibanCodeFilterStrategies) {
        this.ibanCodeFilterStrategies = ibanCodeFilterStrategies;
    }

    public boolean isOnlyValidIBANCodes() {
        return onlyValidIBANCodes;
    }

    public void setOnlyValidIBANCodes(boolean onlyValidIBANCodes) {
        this.onlyValidIBANCodes = onlyValidIBANCodes;
    }

    public boolean isAllowSpaces() {
        return allowSpaces;
    }

    public void setAllowSpaces(boolean allowSpaces) {
        this.allowSpaces = allowSpaces;
    }

}