package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.enums.FilterType;

public class FilterProfile {

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("identifiers")
    @Expose
    private Identifiers identifiers;

    /**
     * Determines if a filter is enabled by seeing if it is present in the filter profile.
     * @param filterType The {@link FilterType type} of the filter.
     * @return <code>true</code> if the filter is enabled; otherwise <code>false</code>.
     */
    public boolean isFilterEnabled(FilterType filterType) {

        boolean enabled = false;

        if(filterType == FilterType.AGE) enabled = (identifiers.getAge() != null);
        if(filterType == FilterType.LOCATION_CITY) enabled = (identifiers.getCity() != null);
        if(filterType == FilterType.LOCATION_COUNTY) enabled = (identifiers.getCounty() != null);
        if(filterType == FilterType.CREDIT_CARD) enabled = (identifiers.getCreditCard() != null);
        if(filterType == FilterType.DATE) enabled = (identifiers.getDate() != null);
        if(filterType == FilterType.EMAIL_ADDRESS) enabled = (identifiers.getEmailAddress() != null);
        if(filterType == FilterType.IDENTIFIER) enabled = (identifiers.getIdentifier() != null);
        if(filterType == FilterType.IP_ADDRESS) enabled = (identifiers.getIpAddress() != null);
        if(filterType == FilterType.NER_ENTITY) enabled = (identifiers.getNer() != null);
        if(filterType == FilterType.PHONE_NUMBER) enabled = (identifiers.getPhoneNumber() != null);
        if(filterType == FilterType.PHONE_NUMBER_EXTENSION) enabled = (identifiers.getPhoneNumberExtension() != null);
        if(filterType == FilterType.SSN) enabled = (identifiers.getSsn() != null);
        if(filterType == FilterType.SURNAME) enabled = (identifiers.getSurname() != null);
        if(filterType == FilterType.LOCATION_STATE) enabled = (identifiers.getState() != null);
        if(filterType == FilterType.STATE_ABBREVIATION) enabled = (identifiers.getStateAbbreviation() != null);
        if(filterType == FilterType.URL) enabled = (identifiers.getUrl() != null);
        if(filterType == FilterType.VIN) enabled = (identifiers.getVin() != null);
        if(filterType == FilterType.ZIP_CODE) enabled = (identifiers.getZipCode() != null);

        return enabled;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Identifiers getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Identifiers identifiers) {
        this.identifiers = identifiers;
    }

}