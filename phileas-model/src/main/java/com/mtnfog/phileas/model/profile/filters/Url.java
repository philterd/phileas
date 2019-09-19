package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.UrlFilterStrategy;

import java.util.List;

public class Url {

    @SerializedName("urlFilterStrategies")
    @Expose
    private List<UrlFilterStrategy> urlFilterStrategies;

    public List<UrlFilterStrategy> getUrlFilterStrategies() {
        return urlFilterStrategies;
    }

    public void setUrlFilterStrategies(List<UrlFilterStrategy> urlFilterStrategies) {
        this.urlFilterStrategies = urlFilterStrategies;
    }

}