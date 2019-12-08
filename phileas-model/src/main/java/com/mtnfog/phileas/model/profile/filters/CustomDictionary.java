package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy;

import java.util.List;

public class CustomDictionary {

    @SerializedName("type")
    @Expose
    private String type;

    @SerializedName("terms")
    @Expose
    private List<String> terms;

    @SerializedName("customFilterStrategies")
    @Expose
    private List<CustomDictionaryFilterStrategy> customDictionaryFilterStrategies;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public List<CustomDictionaryFilterStrategy> getCustomDictionaryFilterStrategies() {
        return customDictionaryFilterStrategies;
    }

    public void setCustomDictionaryFilterStrategies(List<CustomDictionaryFilterStrategy> customDictionaryFilterStrategies) {
        this.customDictionaryFilterStrategies = customDictionaryFilterStrategies;
    }

}
