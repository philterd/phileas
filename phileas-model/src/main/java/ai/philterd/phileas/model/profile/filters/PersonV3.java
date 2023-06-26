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
package ai.philterd.phileas.model.profile.filters;

import ai.philterd.phileas.model.profile.filters.strategies.ai.PersonsFilterStrategy;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PersonV3 extends AbstractFilter {

    @SerializedName("personFilterStrategies")
    @Expose
    private List<PersonsFilterStrategy> personFilterStrategies;

    @SerializedName("thresholds")
    @Expose
    private Map<String, Double> thresholds = new LinkedHashMap<>();

    @SerializedName("model")
    @Expose
    private String model = "/opt/philter/en-ner-person.bin";

    public List<PersonsFilterStrategy> getNerStrategies() {
        return personFilterStrategies;
    }

    public void setPersonFilterStrategies(List<PersonsFilterStrategy> personFilterStrategies) {
        this.personFilterStrategies = personFilterStrategies;
    }

    public Map<String, Double> getThresholds() {
        return thresholds;
    }

    public void setThresholds(Map<String, Double> thresholds) {
        this.thresholds = thresholds;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

}