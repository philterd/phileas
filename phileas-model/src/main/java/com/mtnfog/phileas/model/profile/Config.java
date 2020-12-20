package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.config.Pdf;
import com.mtnfog.phileas.model.profile.config.Splitting;

public class Config {

    @SerializedName("splitting")
    @Expose
    private Splitting splitting = new Splitting();

    @SerializedName("pdf")
    @Expose
    private Pdf pdf = new Pdf();

    public Splitting getSplitting() {
        return splitting;
    }

    public void setSplitting(Splitting splitting) {
        this.splitting = splitting;
    }

    public Pdf getPdf() {
        return pdf;
    }

    public void setPdf(Pdf pdf) {
        this.pdf = pdf;
    }

}
