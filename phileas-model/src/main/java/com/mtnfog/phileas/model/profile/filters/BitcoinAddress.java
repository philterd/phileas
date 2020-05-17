package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.BitcoinAddressFilterStrategy;

import java.util.List;

public class BitcoinAddress extends AbstractFilter {

    @SerializedName("bitcoinFilterStrategies")
    @Expose
    private List<BitcoinAddressFilterStrategy> bitcoinFilterStrategies;

    public List<BitcoinAddressFilterStrategy> getBitcoinFilterStrategies() {
        return bitcoinFilterStrategies;
    }

    public void setBitcoinFilterStrategies(List<BitcoinAddressFilterStrategy> bitcoinFilterStrategies) {
        this.bitcoinFilterStrategies = bitcoinFilterStrategies;
    }

}