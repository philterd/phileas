package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.StreetAddressFilterStrategy;

import java.util.List;

public class StreetAddress extends AbstractFilter {

    @SerializedName("streetAddressFilterStrategies")
    @Expose
    private List<StreetAddressFilterStrategy> streetAddressFilterStrategies;

    public List<StreetAddressFilterStrategy> getStreetAddressFilterStrategies() {
        return streetAddressFilterStrategies;
    }

    public void setStreetAddressFilterStrategies(List<StreetAddressFilterStrategy> streetAddressFilterStrategies) {
        this.streetAddressFilterStrategies = streetAddressFilterStrategies;
    }

}