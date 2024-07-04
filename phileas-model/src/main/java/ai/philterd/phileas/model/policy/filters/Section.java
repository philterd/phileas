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
import ai.philterd.phileas.model.policy.filters.strategies.rules.SectionFilterStrategy;

import java.util.List;

public class Section extends AbstractFilter {

    @SerializedName("sectionFilterStrategies")
    @Expose
    private List<SectionFilterStrategy> sectionFilterStrategies;

    @SerializedName("startPattern")
    @Expose
    private String startPattern;

    @SerializedName("endPattern")
    @Expose
    private String endPattern;

    public List<SectionFilterStrategy> getSectionFilterStrategies() {
        return sectionFilterStrategies;
    }

    public void setSectionFilterStrategies(List<SectionFilterStrategy> sectionFilterStrategies) {
        this.sectionFilterStrategies = sectionFilterStrategies;
    }

    public String getStartPattern() {
        return startPattern;
    }

    public void setStartPattern(String startPattern) {
        this.startPattern = startPattern;
    }

    public String getEndPattern() {
        return endPattern;
    }

    public void setEndPattern(String endPattern) {
        this.endPattern = endPattern;
    }

}