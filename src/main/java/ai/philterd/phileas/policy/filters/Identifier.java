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
import ai.philterd.phileas.services.strategies.rules.IdentifierFilterStrategy;

import java.util.List;

public class Identifier extends AbstractFilter {

    /**
     * The default regex pattern to use if none is provided in the policy.
     */
    public static final String DEFAULT_IDENTIFIER_REGEX = "\\b[A-Z0-9_-]{6,}\\b";

    @SerializedName("identifierFilterStrategies")
    @Expose
    private List<IdentifierFilterStrategy> identifierFilterStrategies;

    @SerializedName("pattern")
    @Expose
    private String pattern = DEFAULT_IDENTIFIER_REGEX;

    @SerializedName("groupNumber")
    @Expose
    private int groupNumber = 0;

    @SerializedName("caseSensitive")
    @Expose
    private boolean caseSensitive = true;

    @SerializedName("classification")
    @Expose
    private String classification = "custom-identifier";

    public List<IdentifierFilterStrategy> getIdentifierFilterStrategies() {
        return identifierFilterStrategies;
    }

    public void setIdentifierFilterStrategies(List<IdentifierFilterStrategy> identifierFilterStrategies) {
        this.identifierFilterStrategies = identifierFilterStrategies;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public int getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(int groupNumber) {
        this.groupNumber = groupNumber;
    }

}