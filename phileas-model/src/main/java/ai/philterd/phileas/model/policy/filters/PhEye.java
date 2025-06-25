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
package ai.philterd.phileas.model.policy.filters;

import ai.philterd.phileas.model.policy.filters.pheye.AbstractPhEye;
import ai.philterd.phileas.model.policy.filters.strategies.ai.PhEyeFilterStrategy;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhEye extends AbstractPhEye {

    @SerializedName("phEyeFilterStrategies")
    @Expose
    private List<PhEyeFilterStrategy> phEyeFilterStrategies;

    @SerializedName("removePunctuation")
    @Expose
    private boolean removePunctuation = false;

    @SerializedName("thresholds")
    @Expose
    private Map<String, Double> thresholds = new LinkedHashMap<>();

    public List<PhEyeFilterStrategy> getPhEyeFilterStrategies() {
        return phEyeFilterStrategies;
    }

    public void setPhEyeFilterStrategies(List<PhEyeFilterStrategy> phEyeFilterStrategies) {
        this.phEyeFilterStrategies = phEyeFilterStrategies;
    }

    public Map<String, Double> getThresholds() {
        return thresholds;
    }

    public void setThresholds(Map<String, Double> thresholds) {
        this.thresholds = thresholds;
    }

    public boolean isRemovePunctuation() {
        return removePunctuation;
    }

    public void setRemovePunctuation(boolean removePunctuation) {
        this.removePunctuation = removePunctuation;
    }

}