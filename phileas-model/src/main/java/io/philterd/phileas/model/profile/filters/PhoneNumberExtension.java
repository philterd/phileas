package io.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.philterd.phileas.model.profile.filters.strategies.rules.PhoneNumberExtensionFilterStrategy;

import java.util.List;

public class PhoneNumberExtension extends AbstractFilter {

    @SerializedName("phoneNumberExtensionFilterStrategies")
    @Expose
    private List<PhoneNumberExtensionFilterStrategy> phoneNumberExtensionFilterStrategies;

    public List<PhoneNumberExtensionFilterStrategy> getPhoneNumberExtensionFilterStrategies() {
        return phoneNumberExtensionFilterStrategies;
    }

    public void setPhoneNumberExtensionFilterStrategies(List<PhoneNumberExtensionFilterStrategy> phoneNumberExtensionFilterStrategies) {
        this.phoneNumberExtensionFilterStrategies = phoneNumberExtensionFilterStrategies;
    }

}