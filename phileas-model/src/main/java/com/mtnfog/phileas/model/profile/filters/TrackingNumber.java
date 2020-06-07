package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.TrackingNumberFilterStrategy;

import java.util.List;

public class TrackingNumber extends AbstractFilter {

    @SerializedName("trackingNumberFilterStrategies")
    @Expose
    private List<TrackingNumberFilterStrategy> trackingNumberFilterStrategies;

    public List<TrackingNumberFilterStrategy> getTrackingNumberFilterStrategies() {
        return trackingNumberFilterStrategies;
    }

    public void setTrackingNumberFilterStrategies(List<TrackingNumberFilterStrategy> trackingNumberFilterStrategies) {
        this.trackingNumberFilterStrategies = trackingNumberFilterStrategies;
    }

}