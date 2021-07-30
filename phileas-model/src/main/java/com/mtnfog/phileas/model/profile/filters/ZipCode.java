package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.ZipCodeFilterStrategy;

import java.util.List;

public class ZipCode extends AbstractFilter {

    @SerializedName("zipCodeFilterStrategy")
    @Expose
    private List<ZipCodeFilterStrategy> zipCodeFilterStrategies;

    @SerializedName("requireDelimiter")
    @Expose
    private boolean requireDelimiter = false;

    public List<ZipCodeFilterStrategy> getZipCodeFilterStrategies() {
        return zipCodeFilterStrategies;
    }

    public void setZipCodeFilterStrategies(List<ZipCodeFilterStrategy> zipCodeFilterStrategies) {
        this.zipCodeFilterStrategies = zipCodeFilterStrategies;
    }

    public boolean isRequireDelimiter() {
        return requireDelimiter;
    }

    public void setRequireDelimiter(boolean requireDelimiter) {
        this.requireDelimiter = requireDelimiter;
    }

}