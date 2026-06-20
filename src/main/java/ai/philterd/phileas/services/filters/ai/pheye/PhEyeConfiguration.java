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
package ai.philterd.phileas.services.filters.ai.pheye;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;

public class PhEyeConfiguration {

    private String endpoint;
    private String bearerToken;
    private int timeout;
    private int maxIdleConnections;
    private int keepAliveDurationMs;
    private Collection<String> labels;

    // Optional path to a local GLiNER model directory. When set, detection runs
    // locally via ONNX Runtime instead of calling the remote endpoint.
    private String modelPath;

    public PhEyeConfiguration(final String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public void setMaxIdleConnections(int maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    public int getKeepAliveDurationMs() {
        return keepAliveDurationMs;
    }

    public void setKeepAliveDurationMs(int keepAliveDurationMs) {
        this.keepAliveDurationMs = keepAliveDurationMs;
    }

    public Collection<String> getLabels() {
        if(CollectionUtils.isNotEmpty(labels)) {
            return labels;
        } else {
            // The default ph-eye English model (ph-eye-pii-en-small and the other
            // ph-eye-pii-en-* sizes) is a GLiNER model trained on the label "name".
            // GLiNER also responds to "Person", but "name" is the model's trained
            // prompt, so it is the default. PhEyeFilter maps both to FilterType.PERSON.
            return List.of("name");
        }
    }

    public void setLabels(Collection<String> labels) {
        this.labels = labels;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

}
