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
package ai.philterd.phileas.model.policy;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class FPE {

    @SerializedName("key")
    @Expose
    private String key;

    @SerializedName("tweak")
    @Expose
    private String tweak;

    /**
     * Empty constructor needed for serialization.
     */
    public FPE() {

    }

    public FPE(String key, String tweak) {
        this.key = key;
        this.tweak = tweak;
    }

    public String getKey() {

        if(key.startsWith("env:")) {

            final String envVarName = key.substring(4);
            return System.getenv(envVarName);

        }

        return key;

    }

    public String getTweak() {

        if(tweak.startsWith("env:")) {

            final String envVarName = tweak.substring(4);
            return System.getenv(envVarName);

        }

        return tweak;

    }

}
