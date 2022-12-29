package com.mtnfog.phileas.model.profile.config;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Splitting {

    @SerializedName("enabled")
    @Expose
    private boolean enabled = false;

    @SerializedName("threshold")
    @Expose
    private int threshold = 10000;

    @SerializedName("method")
    @Expose
    private String method = "newline";

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

}
