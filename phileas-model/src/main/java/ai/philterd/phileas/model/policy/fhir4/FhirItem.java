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
package ai.philterd.phileas.model.policy.fhir4;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class FhirItem {

    public static final String FHIR_ITEM_REPLACEMENT_STRATEGY_CRYPTO_REPLACE = "CRYPTO_REPLACE";
    public static final String FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE = "DELETE";
    public static final String FHIR_ITEM_REPLACEMENT_STRATEGY_SHIFT = "SHIFT";
    public static final String FHIR_ITEM_REPLACEMENT_STRATEGY_TRUNCATE = "TRUNCATE";

    @SerializedName("path")
    @Expose
    private String path;

    @SerializedName("replacementStrategy")
    @Expose
    private String replacementStrategy = FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE;

    /**
     * Empty constructor needed for serialization.
     */
    public FhirItem() {

    }

    public FhirItem(String path, String replacementStrategy) {

        this.path = path;
        this.replacementStrategy = replacementStrategy;

    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getReplacementStrategy() {
        return replacementStrategy;
    }

    public void setReplacementStrategy(String replacementStrategy) {
        this.replacementStrategy = replacementStrategy;
    }

}
