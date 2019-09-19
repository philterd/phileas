package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.IpAddressFilterStrategy;

import java.util.List;

public class IpAddress {

    @SerializedName("ipAddressFilterStrategies")
    @Expose
    private List<IpAddressFilterStrategy> ipAddressFilterStrategies;

    public List<IpAddressFilterStrategy> getIpAddressFilterStrategies() {
        return ipAddressFilterStrategies;
    }

    public void setIpAddressFilterStrategies(List<IpAddressFilterStrategy> ipAddressFilterStrategies) {
        this.ipAddressFilterStrategies = ipAddressFilterStrategies;
    }

}