package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.HospitalAbbreviationFilterStrategy;

import java.util.List;

public class HospitalAbbreviation {

    @SerializedName("hospitalAbbreviationFilterStrategies")
    @Expose
    private List<HospitalAbbreviationFilterStrategy> hospitalAbbreviationFilterStrategies;

    public List<HospitalAbbreviationFilterStrategy> getHospitalAbbreviationFilterStrategies() {
        return hospitalAbbreviationFilterStrategies;
    }

    public void setHospitalAbbreviationFilterStrategies(List<HospitalAbbreviationFilterStrategy> hospitalAbbreviationFilterStrategies) {
        this.hospitalAbbreviationFilterStrategies = hospitalAbbreviationFilterStrategies;
    }

}