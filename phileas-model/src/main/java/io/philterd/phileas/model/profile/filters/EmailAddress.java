package io.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.philterd.phileas.model.profile.filters.strategies.rules.EmailAddressFilterStrategy;

import java.util.List;

public class EmailAddress extends AbstractFilter {

    @SerializedName("emailAddressFilterStrategies")
    @Expose
    private List<EmailAddressFilterStrategy> emailAddressFilterStrategies;

    public List<EmailAddressFilterStrategy> getEmailAddressFilterStrategies() {
        return emailAddressFilterStrategies;
    }

    public void setEmailAddressFilterStrategies(List<EmailAddressFilterStrategy> emailAddressFilterStrategies) {
        this.emailAddressFilterStrategies = emailAddressFilterStrategies;
    }

}