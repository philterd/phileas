package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.IdentifierFilterStrategy;

import java.util.List;

public class Identifier {

    @SerializedName("identifierFilterStrategies")
    @Expose
    private List<IdentifierFilterStrategy> identifierFilterStrategies;

    public List<IdentifierFilterStrategy> getIdentifierFilterStrategies() {
        return identifierFilterStrategies;
    }

    public void setIdentifierFilterStrategies(List<IdentifierFilterStrategy> identifierFilterStrategies) {
        this.identifierFilterStrategies = identifierFilterStrategies;
    }

}