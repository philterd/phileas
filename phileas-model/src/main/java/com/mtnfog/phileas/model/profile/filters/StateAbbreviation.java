package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.StateAbbreviationsFilterStrategy;

import java.util.List;

public class StateAbbreviation {

    @SerializedName("stateAbbreviationFilterStrategies")
    @Expose
    private List<StateAbbreviationsFilterStrategy> stateAbbreviationFilterStrategies;

    public List<StateAbbreviationsFilterStrategy> getStateAbbreviationsFilterStrategies() {
        return stateAbbreviationFilterStrategies;
    }

    public void setStateAbbreviationsFilterStrategies(List<StateAbbreviationsFilterStrategy> stateAbbreviationFilterStrategies) {
        this.stateAbbreviationFilterStrategies = stateAbbreviationFilterStrategies;
    }
}