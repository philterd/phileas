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
package ai.philterd.phileas.model.policy.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.policy.filters.strategies.rules.TrackingNumberFilterStrategy;

import java.util.List;

public class TrackingNumber extends AbstractFilter {

    @SerializedName("ups")
    @Expose
    private boolean ups = true;

    @SerializedName("fedex")
    @Expose
    private boolean fedex = true;

    @SerializedName("usps")
    @Expose
    private boolean usps = true;

    @SerializedName("allowSpaces")
    @Expose
    private boolean allowSpaces = false;

    @SerializedName("trackingNumberFilterStrategies")
    @Expose
    private List<TrackingNumberFilterStrategy> trackingNumberFilterStrategies;

    public List<TrackingNumberFilterStrategy> getTrackingNumberFilterStrategies() {
        return trackingNumberFilterStrategies;
    }

    public void setTrackingNumberFilterStrategies(List<TrackingNumberFilterStrategy> trackingNumberFilterStrategies) {
        this.trackingNumberFilterStrategies = trackingNumberFilterStrategies;
    }

    public boolean isUps() {
        return ups;
    }

    public void setUps(boolean ups) {
        this.ups = ups;
    }

    public boolean isFedex() {
        return fedex;
    }

    public void setFedex(boolean fedex) {
        this.fedex = fedex;
    }

    public boolean isUsps() {
        return usps;
    }

    public void setUsps(boolean usps) {
        this.usps = usps;
    }

    public boolean isAllowSpaces() {
        return allowSpaces;
    }

    public void setAllowSpaces(boolean allowSpaces) {
        this.allowSpaces = allowSpaces;
    }

}