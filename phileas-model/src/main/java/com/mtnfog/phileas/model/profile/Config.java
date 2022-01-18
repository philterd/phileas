package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.config.Analysis;
import com.mtnfog.phileas.model.profile.config.Pdf;

public class Config {

    @SerializedName("pdf")
    @Expose
    private Pdf pdf = new Pdf();

    @SerializedName("postFilters")
    @Expose
    private PostFilters postFilters = new PostFilters();

    @SerializedName("analysis")
    @Expose
    private Analysis analysis = new Analysis();

    public Pdf getPdf() {
        return pdf;
    }

    public void setPdf(Pdf pdf) {
        this.pdf = pdf;
    }

    public PostFilters getPostFilters() {
        return postFilters;
    }

    public void setPostFilters(PostFilters postFilters) {
        this.postFilters = postFilters;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }
}
