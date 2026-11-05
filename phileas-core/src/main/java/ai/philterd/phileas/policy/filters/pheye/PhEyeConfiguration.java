package ai.philterd.phileas.policy.filters.pheye;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Optional;

public class PhEyeConfiguration {

    private static final String DEFAULT_ENDPOINT = "http://philter-ph-eye-1:5000/";

    @SerializedName("endpoint")
    @Expose
    protected String endpoint = defaultEndpoint();

    // Left null unless the policy sets it. getBearerToken() falls back to PHEYE_BEARER_TOKEN
    // only for the endpoint that token was issued for (issue #444).
    @SerializedName("bearerToken")
    @Expose
    protected String bearerToken;

    @SerializedName("timeout")
    @Expose
    protected int timeout = Integer.parseInt(Optional.ofNullable(System.getenv("PHEYE_TIMEOUT")).orElse("600"));

    @SerializedName("maxIdleConnections")
    @Expose
    protected int maxIdleConnections = 30;

    @SerializedName("labels")
    @Expose
    protected Collection<String> labels;

    // Optional path to a local GLiNER model directory (ONNX model, tokenizer, and
    // gliner_config.json). When set, detection runs locally via ONNX Runtime instead
    // of calling the remote endpoint. Emitted by phisql's DETECT PHEYE ... MODEL clause.
    @SerializedName("modelPath")
    @Expose
    protected String modelPath;

    // Minimum span confidence for the local model. Ignored by the remote PhEye service.
    @SerializedName("threshold")
    @Expose
    protected double threshold = 0.5;

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

    /**
     * Gets the bearer token to send to the endpoint. A token the policy sets is always used.
     * Otherwise the {@code PHEYE_BEARER_TOKEN} environment variable is used, but only when the
     * endpoint is the default one ({@code PHEYE_ENDPOINT}, ignoring a trailing slash), so the
     * deployment's token is never sent to an endpoint a policy names.
     * @return The bearer token, or <code>null</code> if none applies.
     */
    public String getBearerToken() {

        if (bearerToken != null) {
            return bearerToken;
        }

        if (endpoint != null && StringUtils.stripEnd(endpoint, "/").equals(StringUtils.stripEnd(defaultEndpoint(), "/"))) {
            return System.getenv("PHEYE_BEARER_TOKEN");
        }

        return null;

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

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    private static String defaultEndpoint() {
        return Optional.ofNullable(System.getenv("PHEYE_ENDPOINT")).orElse(DEFAULT_ENDPOINT);
    }

}
