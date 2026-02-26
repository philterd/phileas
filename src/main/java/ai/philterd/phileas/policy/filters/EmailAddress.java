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

import ai.philterd.phileas.services.strategies.rules.EmailAddressFilterStrategy;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class EmailAddress extends AbstractFilter {

    @SerializedName("onlyStrictMatches")
    @Expose
    protected boolean onlyStrictMatches = true;

    @SerializedName("onlyValidTLDs")
    @Expose
    protected boolean onlyValidTLDs = false;

    @SerializedName("emailAddressFilterStrategies")
    @Expose
    private List<EmailAddressFilterStrategy> emailAddressFilterStrategies;

    public List<EmailAddressFilterStrategy> getEmailAddressFilterStrategies() {
        return emailAddressFilterStrategies;
    }

    public void setEmailAddressFilterStrategies(List<EmailAddressFilterStrategy> emailAddressFilterStrategies) {
        this.emailAddressFilterStrategies = emailAddressFilterStrategies;
    }

    public boolean isOnlyStrictMatches() {
        return onlyStrictMatches;
    }

    public void setOnlyStrictMatches(boolean value) {
        onlyStrictMatches = value;
    }

    public boolean isOnlyValidTLDs() {
        return onlyValidTLDs;
    }

    public void setOnlyValidTLDs(boolean onlyValidTLDs) {
        this.onlyValidTLDs = onlyValidTLDs;
    }

}