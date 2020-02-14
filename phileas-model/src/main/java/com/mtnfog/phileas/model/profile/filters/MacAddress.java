package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.MacAddressFilterStrategy;

import java.util.List;

public class MacAddress extends AbstractFilter {

    @SerializedName("macAddressFilterStrategies")
    @Expose
    private List<MacAddressFilterStrategy> macAddressFilterStrategies;

    public List<MacAddressFilterStrategy> getMacAddressFilterStrategies() {
        return macAddressFilterStrategies;
    }

    public void setMacAddressFilterStrategies(List<MacAddressFilterStrategy> ipAddressFilterStrategies) {
        this.macAddressFilterStrategies = macAddressFilterStrategies;
    }

}