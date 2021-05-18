package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.BankRoutingNumberFilterStrategy;

import java.util.List;

public class BankRoutingNumber extends AbstractFilter {

    @SerializedName("bankRoutingNumberFilterStrategies")
    @Expose
    private List<BankRoutingNumberFilterStrategy> bankRoutingNumberFilterStrategies;

    public List<BankRoutingNumberFilterStrategy> getBankRoutingNumberFilterStrategies() {
        return bankRoutingNumberFilterStrategies;
    }

    public void setBankRoutingNumberFilterStrategies(List<BankRoutingNumberFilterStrategy> bankRoutingNumberFilterStrategies) {
        this.bankRoutingNumberFilterStrategies = bankRoutingNumberFilterStrategies;
    }

}