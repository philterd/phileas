package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.CreditCardFilterStrategy;

import java.util.List;

public class CreditCard extends AbstractFilter {

    @SerializedName("creditCardFilterStrategies")
    @Expose
    private List<CreditCardFilterStrategy> creditCardFilterStrategies;

    public List<CreditCardFilterStrategy> getCreditCardFilterStrategies() {
        return creditCardFilterStrategies;
    }

    public void setCreditCardFilterStrategies(List<CreditCardFilterStrategy> creditCardFilterStrategies) {
        this.creditCardFilterStrategies = creditCardFilterStrategies;
    }

}