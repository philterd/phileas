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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.filtering.SensitivityLevel;
import ai.philterd.phileas.services.strategies.dynamic.FirstNameFilterStrategy;

import java.util.List;

public class FirstName extends AbstractFilter {

    @SerializedName("firstNameFilterStrategies")
    @Expose
    private List<FirstNameFilterStrategy> firstNameFilterStrategies;

    @SerializedName("fuzzy")
    @Expose
    private boolean fuzzy = false;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

    @SerializedName("capitalized")
    @Expose
    private boolean capitalized = false;

    public List<FirstNameFilterStrategy> getFirstNameFilterStrategies() {
        return firstNameFilterStrategies;
    }

    public void setFirstNameFilterStrategies(List<FirstNameFilterStrategy> firstNameFilterStrategies) {
        this.firstNameFilterStrategies = firstNameFilterStrategies;
    }

    public SensitivityLevel getSensitivityLevel() {
        return SensitivityLevel.fromName(sensitivity);
    }

    public String getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }

    public boolean isCapitalized() {
        return capitalized;
    }

    public void setCapitalized(boolean capitalized) {
        this.capitalized = capitalized;
    }

    public boolean isFuzzy() {
        return fuzzy;
    }

    public void setFuzzy(boolean fuzzy) {
        this.fuzzy = fuzzy;
    }
}