package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.AgeFilterStrategy;

import java.util.List;

public class Age extends AbstractFilter {

    @SerializedName("ageFilterStrategies")
    @Expose
    private List<AgeFilterStrategy> ageFilterStrategies;

    public List<AgeFilterStrategy> getAgeFilterStrategies() {
        return ageFilterStrategies;
    }

    public void setAgeFilterStrategies(List<AgeFilterStrategy> ageFilterStrategies) {
        this.ageFilterStrategies = ageFilterStrategies;
    }

}