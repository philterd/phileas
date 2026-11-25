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
package ai.philterd.phileas.policy.config;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

public class Review {

    // The probability, from 0.0 to 1.0, that a filter request is selected for human spot review.
    @SerializedName("samplingRate")
    @Expose
    private double samplingRate = 0.0;

    // A second detection pass over the redacted output. Off when absent.
    @SerializedName("leftoverCheck")
    @Expose
    @JsonAdapter(LeftoverCheck.Adapter.class)
    private LeftoverCheck leftoverCheck = new LeftoverCheck();

    /**
     * Refuses a sampling rate outside 0.0 to 1.0.
     * @throws IllegalArgumentException Thrown if the sampling rate is not from 0.0 to 1.0.
     */
    public void validate() {
        // Written so NaN fails too.
        if (!(samplingRate >= 0.0 && samplingRate <= 1.0)) {
            throw new IllegalArgumentException("The review samplingRate " + samplingRate
                    + " is not valid: it must be a number from 0.0 to 1.0.");
        }
    }

    /**
     * The leftover check, never null: a policy without one has the check off.
     * @return The {@link LeftoverCheck}.
     */
    public LeftoverCheck getLeftoverCheck() {
        return leftoverCheck == null ? new LeftoverCheck() : leftoverCheck;
    }

    public void setLeftoverCheck(LeftoverCheck leftoverCheck) {
        this.leftoverCheck = leftoverCheck;
    }

    public double getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(double samplingRate) {
        this.samplingRate = samplingRate;
    }

}
