package io.philterd.phileas.model.profile.config;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Analysis {

    @SerializedName("enabled")
    @Expose
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
