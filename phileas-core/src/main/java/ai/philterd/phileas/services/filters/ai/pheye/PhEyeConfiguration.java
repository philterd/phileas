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
    private String username;
    private String password;
    private int timeout;
    private int maxIdleConnections;
    private int keepAliveDurationMs;
    private Collection<String> labels;
    
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
            return List.of("Person");
        }
    }

    public void setLabels(Collection<String> labels) {
        this.labels = labels;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
