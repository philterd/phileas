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

import ai.philterd.phileas.services.strategies.rules.PhoneNumberFilterStrategy;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PhoneNumber extends AbstractFilter {

    /** The default region used to interpret phone numbers written without an international "+" country code. */
    public static final String DEFAULT_REGION = "US";

    @SerializedName("phoneNumberFilterStrategies")
    @Expose
    private List<PhoneNumberFilterStrategy> phoneNumberFilterStrategies;

    /**
     * Default region(s), ISO 3166-1 alpha-2, used to detect national-format phone numbers written without
     * an international "+" country code. Accepts either a single string or an array of strings; defaults to
     * a single {@value #DEFAULT_REGION}. Numbers with a "+" prefix are detected regardless of this value.
     */
    @SerializedName("region")
    @Expose
    @JsonAdapter(StringOrArrayListDeserializer.class)
    private List<String> region;

    public List<PhoneNumberFilterStrategy> getPhoneNumberFilterStrategies() {
        return phoneNumberFilterStrategies;
    }

    public void setPhoneNumberFilterStrategies(List<PhoneNumberFilterStrategy> phoneNumberFilterStrategies) {
        this.phoneNumberFilterStrategies = phoneNumberFilterStrategies;
    }

    /**
     * Gets the configured regions, or a single {@value #DEFAULT_REGION} when none was set in the policy.
     */
    public List<String> getRegion() {
        if (region == null || region.isEmpty()) {
            return List.of(DEFAULT_REGION);
        }
        return region;
    }

    public void setRegion(List<String> region) {
        this.region = region;
    }

}