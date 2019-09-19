package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.StateFilterStrategy;

import java.util.List;

public class State {

    @SerializedName("stateFilterStrategies")
    @Expose
    private List<StateFilterStrategy> stateFilterStrategies;

    public List<StateFilterStrategy> getStateFilterStrategies() {
        return stateFilterStrategies;
    }

    public void setStateFilterStrategies(List<StateFilterStrategy> stateFilterStrategies) {
        this.stateFilterStrategies = stateFilterStrategies;
    }

}