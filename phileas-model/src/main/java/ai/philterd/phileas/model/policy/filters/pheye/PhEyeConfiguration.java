package ai.philterd.phileas.model.policy.filters.pheye;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Collection;
import java.util.List;

public class PhEyeConfiguration {

    @SerializedName("endpoint")
    @Expose
    protected String endpoint = "http://philter-ph-eye-1:5000/";

    @SerializedName("bearerToken")
    @Expose
    protected String bearerToken;

    @SerializedName("timeout")
    @Expose
    protected int timeout = 600;

    @SerializedName("maxIdleConnections")
    @Expose
    protected int maxIdleConnections = 30;

    @SerializedName("keepAliveDurationMs")
    @Expose
    protected int keepAliveDurationMs = 30;

    @SerializedName("labels")
    @Expose
    protected Collection<String> labels = List.of("Person");

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
        return labels;
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

}
