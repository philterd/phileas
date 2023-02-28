package io.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.philterd.phileas.model.profile.filters.strategies.rules.StateAbbreviationFilterStrategy;

import java.util.List;

public class StateAbbreviation extends AbstractFilter {

    @SerializedName("stateAbbreviationFilterStrategies")
    @Expose
    private List<StateAbbreviationFilterStrategy> stateAbbreviationFilterStrategies;

    public List<StateAbbreviationFilterStrategy> getStateAbbreviationsFilterStrategies() {
        return stateAbbreviationFilterStrategies;
    }

    public void setStateAbbreviationsFilterStrategies(List<StateAbbreviationFilterStrategy> stateAbbreviationFilterStrategies) {
        this.stateAbbreviationFilterStrategies = stateAbbreviationFilterStrategies;
    }
}