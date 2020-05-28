package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.profile.filters.*;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class Identifiers {

    @SerializedName("ner")
    @Expose
    private Ner ner;

    @SerializedName("dictionaries")
    @Expose
    private List<CustomDictionary> customDictionaries;

    @SerializedName("age")
    @Expose
    private Age age;

    @SerializedName("bitcoinAddress")
    @Expose
    private BitcoinAddress bitcoinAddress;

    @SerializedName("creditCard")
    @Expose
    private CreditCard creditCard;

    @SerializedName("date")
    @Expose
    private Date date;

    @SerializedName("driversLicense")
    @Expose
    private DriversLicense driversLicense;

    @SerializedName("emailAddress")
    @Expose
    private EmailAddress emailAddress;

    @SerializedName("ibanCode")
    @Expose
    private IbanCode ibanCode;

    @SerializedName("identifiers")
    @Expose
    private List<Identifier> identifiers;

    @SerializedName("ipAddress")
    @Expose
    private IpAddress ipAddress;

    @SerializedName("macAddress")
    @Expose
    private MacAddress macAddress;

    @SerializedName("passportNumber")
    @Expose
    private PassportNumber passportNumber;

    @SerializedName("phoneNumber")
    @Expose
    private PhoneNumber phoneNumber;

    @SerializedName("phoneNumberExtension")
    @Expose
    private PhoneNumberExtension phoneNumberExtension;

    @SerializedName("sections")
    @Expose
    private List<Section> sections;

    @SerializedName("ssn")
    @Expose
    private Ssn ssn;

    @SerializedName("stateAbbreviation")
    @Expose
    private StateAbbreviation stateAbbreviation;

    @SerializedName("url")
    @Expose
    private Url url;

    @SerializedName("vin")
    @Expose
    private Vin vin;

    @SerializedName("zipCode")
    @Expose
    private ZipCode zipCode;

    @SerializedName("city")
    @Expose
    private City city;

    @SerializedName("county")
    @Expose
    private County county;

    @SerializedName("firstName")
    @Expose
    private FirstName firstName;

    @SerializedName("hospitalAbbreviation")
    @Expose
    private HospitalAbbreviation hospitalAbbreviation;

    @SerializedName("hospital")
    @Expose
    private Hospital hospital;

    @SerializedName("state")
    @Expose
    private State state;

    @SerializedName("surname")
    @Expose
    private Surname surname;

    public Identifiers() {

    }

    /**
     * Determines if a filter exists for a filter profile.
     * @param filterType The {@link FilterType}.
     * @return <code>true</code> if the filter profile contains a strategy for the corresponding
     * {@link FilterType}; otherwise <code>false</code>.
     */
    public boolean hasFilter(FilterType filterType) {

        switch(filterType) {

            case CUSTOM_DICTIONARY:
                if(CollectionUtils.isNotEmpty(this.getCustomDictionaries())) { return true; } break;
            case AGE:
                if(this.getAge() != null) { return true; } break;
            case BITCOIN_ADDRESS:
                if(this.getBitcoinAddress() != null) { return true; } break;
            case LOCATION_CITY:
                if(this.getCity() != null) { return true; } break;
            case LOCATION_COUNTY:
                if(this.getCounty() != null) { return true; } break;
            case CREDIT_CARD:
                if(this.getCreditCard() != null) { return true; } break;
            case DATE:
                if(this.getDate() != null) { return true; } break;
            case DRIVERS_LICENSE_NUMBER:
                if(this.getDriversLicense() != null) { return true; } break;
            case EMAIL_ADDRESS:
                if(this.getEmailAddress() != null) { return true; } break;
            case FIRST_NAME:
                if(this.getFirstName() != null) { return true; } break;
            case HOSPITAL:
                if(this.getHospital() != null) { return true; } break;
            case HOSPITAL_ABBREVIATION:
                if(this.getHospitalAbbreviation() != null) { return true; } break;
            case IBAN_CODE:
                if(this.getIbanCode() != null) { return true; } break;
            case IDENTIFIER:
                if(this.getIdentifiers() != null) { return true; } break;
            case IP_ADDRESS:
                if(this.getIpAddress() != null) { return true; } break;
            case MAC_ADDRESS:
                if(this.getMacAddress() != null) { return true; } break;
            case NER_ENTITY:
                if(this.getNer() != null) { return true; } break;
            case PHONE_NUMBER:
                if(this.getPhoneNumber() != null) { return true; } break;
            case PHONE_NUMBER_EXTENSION:
                if(this.getPhoneNumberExtension() != null) { return true; } break;
            case SECTION:
                if(this.getSections() != null) { return true; } break;
            case SSN:
                if(this.getSsn() != null) { return true; } break;
            case LOCATION_STATE:
                if(this.getState() != null) { return true; } break;
            case STATE_ABBREVIATION:
                if(this.getStateAbbreviation() != null) { return true; } break;
            case SURNAME:
                if(this.getSurname() != null) { return true; } break;
            case URL:
                if(this.getUrl() != null) { return true; } break;
            case VIN:
                if(this.getVin() != null) { return true; } break;
            case ZIP_CODE:
                if(this.getZipCode() != null) { return true; } break;

        }

        return false;

    }

    public ZipCode getZipCode() {
        return zipCode;
    }

    public void setZipCode(ZipCode zipCode) {
        this.zipCode = zipCode;
    }

    public Vin getVin() {
        return vin;
    }

    public void setVin(Vin vin) {
        this.vin = vin;
    }

    public Url getUrl() {
        return url;
    }

    public void setUrl(Url url) {
        this.url = url;
    }

    public StateAbbreviation getStateAbbreviation() {
        return stateAbbreviation;
    }

    public void setStateAbbreviation(StateAbbreviation stateAbbreviation) {
        this.stateAbbreviation = stateAbbreviation;
    }

    public Ssn getSsn() {
        return ssn;
    }

    public void setSsn(Ssn ssn) {
        this.ssn = ssn;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public List<Identifier> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<Identifier> identifiers) {
        this.identifiers = identifiers;
    }

    public EmailAddress getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(EmailAddress emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public CreditCard getCreditCard() {
        return creditCard;
    }

    public void setCreditCard(CreditCard creditCard) {
        this.creditCard = creditCard;
    }

    public Age getAge() {
        return age;
    }

    public void setAge(Age age) {
        this.age = age;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public County getCounty() {
        return county;
    }

    public void setCounty(County county) {
        this.county = county;
    }

    public FirstName getFirstName() {
        return firstName;
    }

    public void setFirstName(FirstName firstName) {
        this.firstName = firstName;
    }

    public HospitalAbbreviation getHospitalAbbreviation() {
        return hospitalAbbreviation;
    }

    public void setHospitalAbbreviation(HospitalAbbreviation hospitalAbbreviation) {
        this.hospitalAbbreviation = hospitalAbbreviation;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Surname getSurname() {
        return surname;
    }

    public void setSurname(Surname surname) {
        this.surname = surname;
    }

    public PhoneNumberExtension getPhoneNumberExtension() {
        return phoneNumberExtension;
    }

    public void setPhoneNumberExtension(PhoneNumberExtension phoneNumberExtension) {
        this.phoneNumberExtension = phoneNumberExtension;
    }

    public Ner getNer() {
        return ner;
    }

    public void setNer(Ner ner) {
        this.ner = ner;
    }

    public List<CustomDictionary> getCustomDictionaries() {
        return customDictionaries;
    }

    public void setCustomDictionaries(List<CustomDictionary> customDictionaries) {
        this.customDictionaries = customDictionaries;
    }

    public List<Section> getSections() {
        return sections;
    }

    public void setSections(List<Section> sections) {
        this.sections = sections;
    }

    public MacAddress getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(MacAddress macAddress) {
        this.macAddress = macAddress;
    }

    public IbanCode getIbanCode() {
        return ibanCode;
    }

    public void setIbanCode(IbanCode ibanCode) {
        this.ibanCode = ibanCode;
    }

    public BitcoinAddress getBitcoinAddress() {
        return bitcoinAddress;
    }

    public void setBitcoinAddress(BitcoinAddress bitcoinAddress) {
        this.bitcoinAddress = bitcoinAddress;
    }

    public DriversLicense getDriversLicense() {
        return driversLicense;
    }

    public void setDriversLicense(DriversLicense driversLicense) {
        this.driversLicense = driversLicense;
    }

    public PassportNumber getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(PassportNumber passportNumber) {
        this.passportNumber = passportNumber;
    }

}
