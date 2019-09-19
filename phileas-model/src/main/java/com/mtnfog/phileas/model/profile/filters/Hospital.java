package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.HospitalFilterStrategy;

import java.util.List;

public class Hospital {

    @SerializedName("hospitalFilterStrategies")
    @Expose
    private List<HospitalFilterStrategy> hospitalFilterStrategies;

    public List<HospitalFilterStrategy> getHospitalFilterStrategies() {
        return hospitalFilterStrategies;
    }

    public void setHospitalFilterStrategies(List<HospitalFilterStrategy> hospitalFilterStrategies) {
        this.hospitalFilterStrategies = hospitalFilterStrategies;
    }

}