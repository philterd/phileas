package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.PhoneNumberFilterStrategy;

import java.util.List;

public class PhoneNumber {

    @SerializedName("phoneNumberFilterStrategies")
    @Expose
    private List<PhoneNumberFilterStrategy> phoneNumberFilterStrategies;

    public List<PhoneNumberFilterStrategy> getPhoneNumberFilterStrategies() {
        return phoneNumberFilterStrategies;
    }

    public void setPhoneNumberFilterStrategies(List<PhoneNumberFilterStrategy> phoneNumberFilterStrategies) {
        this.phoneNumberFilterStrategies = phoneNumberFilterStrategies;
    }

}