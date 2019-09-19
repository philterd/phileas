package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.SurnameFilterStrategy;

import java.util.List;

public class Surname {

    @SerializedName("surnameFilterStrategies")
    @Expose
    private List<SurnameFilterStrategy> surnameFilterStrategies;

    public List<SurnameFilterStrategy> getSurnameFilterStrategies() {
        return surnameFilterStrategies;
    }

    public void setSurnameFilterStrategies(List<SurnameFilterStrategy> surnameFilterStrategies) {
        this.surnameFilterStrategies = surnameFilterStrategies;
    }

}