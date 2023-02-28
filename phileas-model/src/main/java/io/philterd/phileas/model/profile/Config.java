package io.philterd.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.philterd.phileas.model.profile.config.Analysis;
import io.philterd.phileas.model.profile.config.Pdf;
import io.philterd.phileas.model.profile.config.Splitting;

public class Config {

    @SerializedName("splitting")
    @Expose
    private Splitting splitting = new Splitting();

    @SerializedName("pdf")
    @Expose
    private Pdf pdf = new Pdf();

    @SerializedName("postFilters")
    @Expose
    private PostFilters postFilters = new PostFilters();

    @SerializedName("analysis")
    @Expose
    private Analysis analysis = new Analysis();

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
