package io.philterd.phileas.model.profile;

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
