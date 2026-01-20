package ai.philterd.phileas.policy.filters.pheye;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Collection;
import java.util.Optional;

public class PhEyeConfiguration {

    @SerializedName("endpoint")
    @Expose
    protected String endpoint = Optional.ofNullable(System.getenv("PHEYE_ENDPOINT")).orElse("http://philter-ph-eye-1:5000/");

    @SerializedName("bearerToken")
    @Expose
    protected String bearerToken = System.getenv("PHEYE_BEARER_TOKEN");

    @SerializedName("timeout")
    @Expose
    protected int timeout = Integer.parseInt(Optional.ofNullable(System.getenv("PHEYE_TIMEOUT")).orElse("600"));

    @SerializedName("maxIdleConnections")
    @Expose
    protected int maxIdleConnections = 30;

    @SerializedName("labels")
    @Expose
    protected Collection<String> labels;

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
