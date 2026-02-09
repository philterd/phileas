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

import ai.philterd.phileas.services.strategies.rules.ZipCodeFilterStrategy;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ZipCode extends AbstractFilter {

    @SerializedName("zipCodeFilterStrategy")
    @Expose
    private List<ZipCodeFilterStrategy> zipCodeFilterStrategies;

    @SerializedName("requireDelimiter")
    @Expose
    private boolean requireDelimiter = false;

    @SerializedName("validate")
    @Expose
    private boolean validate = false;

    public List<ZipCodeFilterStrategy> getZipCodeFilterStrategies() {
        return zipCodeFilterStrategies;
    }

    public void setZipCodeFilterStrategies(List<ZipCodeFilterStrategy> zipCodeFilterStrategies) {
        this.zipCodeFilterStrategies = zipCodeFilterStrategies;
    }

    public boolean isRequireDelimiter() {
        return requireDelimiter;
    }

    public void setRequireDelimiter(boolean requireDelimiter) {
        this.requireDelimiter = requireDelimiter;
    }

    public boolean isValidate() {
        return validate;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

}