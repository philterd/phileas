package com.mtnfog.phileas.model.profile.fhir4;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class FhirItem {

    public static final String FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE = "DELETE";

    @SerializedName("path")
    @Expose
    private String path;

    @SerializedName("replacementStrategy")
    @Expose
    private String replacementStrategy = FHIR_ITEM_REPLACEMENT_STRATEGY_DELETE;

    public FhirItem() {

    }

    public FhirItem(String path, String replacementStrategy) {

        this.path = path;
        this.replacementStrategy = replacementStrategy;

    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getReplacementStrategy() {
        return replacementStrategy;
    }

    public void setReplacementStrategy(String replacementStrategy) {
        this.replacementStrategy = replacementStrategy;
    }

}
