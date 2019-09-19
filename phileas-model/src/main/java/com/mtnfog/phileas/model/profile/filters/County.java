package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.CountyFilterStrategy;

import java.util.List;

public class County {

    @SerializedName("countyFilterStrategies")
    @Expose
    private List<CountyFilterStrategy> countyFilterStrategies;

    public List<CountyFilterStrategy> getCountyFilterStrategies() {
        return countyFilterStrategies;
    }

    public void setCountyFilterStrategies(List<CountyFilterStrategy> countyFilterStrategies) {
        this.countyFilterStrategies = countyFilterStrategies;
    }

}