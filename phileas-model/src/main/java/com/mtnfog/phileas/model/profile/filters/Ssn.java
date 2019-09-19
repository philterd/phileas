package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.SsnFilterStrategy;

import java.util.List;

public class Ssn {

    @SerializedName("ssnFilterStrategies")
    @Expose
    private List<SsnFilterStrategy> ssnFilterStrategies;

    public List<SsnFilterStrategy> getSsnFilterStrategies() {
        return ssnFilterStrategies;
    }

    public void setSsnFilterStrategies(List<SsnFilterStrategy> ssnFilterStrategies) {
        this.ssnFilterStrategies = ssnFilterStrategies;
    }
}