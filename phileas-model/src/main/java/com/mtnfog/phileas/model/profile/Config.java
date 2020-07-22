package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.config.Splitting;

public class Config {

    @SerializedName("splitting")
    @Expose
    private Splitting splitting = new Splitting();

    public Splitting getSplitting() {
        return splitting;
    }

    public void setSplitting(Splitting splitting) {
        this.splitting = splitting;
    }

}
