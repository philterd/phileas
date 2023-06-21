/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy;

import java.util.List;

public class CustomDictionary extends AbstractFilter {

    @SerializedName("classification")
    @Expose
    private String classification;

    @SerializedName("terms")
    @Expose
    private List<String> terms;

    @SerializedName("files")
    @Expose
    private List<String> files;

    @SerializedName("fuzzy")
    @Expose
    private boolean fuzzy = false;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

    @SerializedName("customFilterStrategies")
    @Expose
    private List<CustomDictionaryFilterStrategy> customDictionaryFilterStrategies;

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public List<CustomDictionaryFilterStrategy> getCustomDictionaryFilterStrategies() {
        return customDictionaryFilterStrategies;
    }

    public void setCustomDictionaryFilterStrategies(List<CustomDictionaryFilterStrategy> customDictionaryFilterStrategies) {
        this.customDictionaryFilterStrategies = customDictionaryFilterStrategies;
    }

    public String getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public boolean isFuzzy() {
        return fuzzy;
    }

    public void setFuzzy(boolean fuzzy) {
        this.fuzzy = fuzzy;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

}
