package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.CityFilterStrategy;

import java.util.List;

public class City {

    @SerializedName("cityFilterStrategies")
    @Expose
    private List<CityFilterStrategy> cityFilterStrategies;

    public List<CityFilterStrategy> getCityFilterStrategies() {
        return cityFilterStrategies;
    }

    public void setCityFilterStrategies(List<CityFilterStrategy> cityFilterStrategies) {
        this.cityFilterStrategies = cityFilterStrategies;
    }

}