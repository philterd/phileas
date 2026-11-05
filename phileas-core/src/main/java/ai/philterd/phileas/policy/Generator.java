/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.policy;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * A named replacement generator referenced by a {@code MAP_REPLACE} filter strategy. A generator
 * produces a replacement for a detected value that is absent from the strategy's lookup table.
 * Generators target a local model endpoint inside the deployment boundary so detected values are
 * not sent to a third party.
 */
public class Generator {

    /** Generator backend. {@code ollama} calls a local Ollama-compatible generate endpoint. */
    public static final String TYPE_OLLAMA = "ollama";

    @SerializedName("type")
    @Expose
    private String type;

    @SerializedName("endpoint")
    @Expose
    private String endpoint;

    @SerializedName("model")
    @Expose
    private String model;

    @SerializedName("prompt")
    @Expose
    private String prompt;

    @SerializedName("timeoutMs")
    @Expose
    private Integer timeoutMs;

    /**
     * Empty constructor needed for serialization.
     */
    public Generator() {

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

}
