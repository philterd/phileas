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

import ai.philterd.phileas.services.strategies.dynamic.HospitalFilterStrategy;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Hospital extends AbstractDictionaryBackedFilter {

    @SerializedName("hospitalFilterStrategies")
    @Expose
    private List<HospitalFilterStrategy> hospitalFilterStrategies;

    public List<HospitalFilterStrategy> getHospitalFilterStrategies() {
        return hospitalFilterStrategies;
    }

    public void setHospitalFilterStrategies(List<HospitalFilterStrategy> hospitalFilterStrategies) {
        this.hospitalFilterStrategies = hospitalFilterStrategies;
    }

}
