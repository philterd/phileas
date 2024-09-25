package ai.philterd.phileas.services.filters.ai.pheye;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;

public class PhEyeConfiguration {

    private String endpoint;
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
    
}
