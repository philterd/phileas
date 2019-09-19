package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.FirstNameFilterStrategy;

import java.util.List;

public class FirstName {

    @SerializedName("firstNameFilterStrategies")
    @Expose
    private List<FirstNameFilterStrategy> firstNameFilterStrategies;

    public List<FirstNameFilterStrategy> getFirstNameFilterStrategies() {
        return firstNameFilterStrategies;
    }

    public void setFirstNameFilterStrategies(List<FirstNameFilterStrategy> firstNameFilterStrategies) {
        this.firstNameFilterStrategies = firstNameFilterStrategies;
    }

}