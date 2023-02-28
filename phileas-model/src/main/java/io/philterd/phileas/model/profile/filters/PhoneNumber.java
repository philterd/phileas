package io.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.philterd.phileas.model.profile.filters.strategies.rules.PhoneNumberFilterStrategy;

import java.util.List;

public class PhoneNumber extends AbstractFilter {

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