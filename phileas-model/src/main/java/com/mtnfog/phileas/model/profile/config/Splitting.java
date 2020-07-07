package com.mtnfog.phileas.model.profile.config;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Splitting {

    @SerializedName("method")
    @Expose
    private String method = "newline";

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

}
