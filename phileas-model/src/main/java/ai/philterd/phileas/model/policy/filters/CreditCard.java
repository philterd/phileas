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
import ai.philterd.phileas.model.policy.filters.strategies.rules.CreditCardFilterStrategy;

import java.util.List;

public class CreditCard extends AbstractFilter {

    @SerializedName("onlyValidCreditCardNumbers")
    @Expose
    protected boolean onlyValidCreditCardNumbers = true;

    @SerializedName("creditCardFilterStrategies")
    @Expose
    private List<CreditCardFilterStrategy> creditCardFilterStrategies;

    @SerializedName("ignoreWhenInUnixTimestamp")
    @Expose private boolean ignoreWhenInUnixTimestamp = false;

    public List<CreditCardFilterStrategy> getCreditCardFilterStrategies() {
        return creditCardFilterStrategies;
    }

    public void setCreditCardFilterStrategies(List<CreditCardFilterStrategy> creditCardFilterStrategies) {
        this.creditCardFilterStrategies = creditCardFilterStrategies;
    }

    public boolean isOnlyValidCreditCardNumbers() {
        return onlyValidCreditCardNumbers;
    }

    public void setOnlyValidCreditCardNumbers(boolean onlyValidCreditCardNumbers) {
        this.onlyValidCreditCardNumbers = onlyValidCreditCardNumbers;
    }

    public boolean isIgnoreWhenInUnixTimestamp() {
        return ignoreWhenInUnixTimestamp;
    }

    public void setIgnoreWhenInUnixTimestamp(boolean ignoreWhenInUnixTimestamp) {
        this.ignoreWhenInUnixTimestamp = ignoreWhenInUnixTimestamp;
    }

}