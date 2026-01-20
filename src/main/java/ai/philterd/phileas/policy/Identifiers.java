/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.policy;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.policy.filters.Age;
import ai.philterd.phileas.policy.filters.BankRoutingNumber;
import ai.philterd.phileas.policy.filters.BitcoinAddress;
import ai.philterd.phileas.policy.filters.City;
import ai.philterd.phileas.policy.filters.County;
import ai.philterd.phileas.policy.filters.CreditCard;
import ai.philterd.phileas.policy.filters.Currency;
import ai.philterd.phileas.policy.filters.CustomDictionary;
import ai.philterd.phileas.policy.filters.Date;
import ai.philterd.phileas.policy.filters.DriversLicense;
import ai.philterd.phileas.policy.filters.EmailAddress;
import ai.philterd.phileas.policy.filters.FirstName;
import ai.philterd.phileas.policy.filters.Hospital;
import ai.philterd.phileas.policy.filters.HospitalAbbreviation;
import ai.philterd.phileas.policy.filters.IbanCode;
import ai.philterd.phileas.policy.filters.Identifier;
import ai.philterd.phileas.policy.filters.IpAddress;
import ai.philterd.phileas.policy.filters.MacAddress;
import ai.philterd.phileas.policy.filters.MedicalCondition;
import ai.philterd.phileas.policy.filters.PassportNumber;
import ai.philterd.phileas.policy.filters.PhEye;
import ai.philterd.phileas.policy.filters.PhoneNumber;
import ai.philterd.phileas.policy.filters.PhoneNumberExtension;
import ai.philterd.phileas.policy.filters.PhysicianName;
import ai.philterd.phileas.policy.filters.Section;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.policy.filters.State;
import ai.philterd.phileas.policy.filters.StateAbbreviation;
import ai.philterd.phileas.policy.filters.StreetAddress;
import ai.philterd.phileas.policy.filters.Surname;
import ai.philterd.phileas.policy.filters.TrackingNumber;
import ai.philterd.phileas.policy.filters.Url;
import ai.philterd.phileas.policy.filters.Vin;
import ai.philterd.phileas.policy.filters.ZipCode;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class Identifiers {

    @SerializedName("person")
    @Expose
    private PhEye phEye;

    @SerializedName("dictionaries")
    @Expose
    private List<CustomDictionary> customDictionaries;

    @SerializedName("age")
    @Expose
    private Age age;

    @SerializedName("bankRoutingNumber")
    @Expose
    private BankRoutingNumber bankRoutingNumber;

    @SerializedName("bitcoinAddress")
    @Expose
    private BitcoinAddress bitcoinAddress;

    @SerializedName("creditCard")
    @Expose
    private CreditCard creditCard;

    @SerializedName("currency")
    @Expose
    private Currency currency;

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

    @SerializedName("physicianName")
    @Expose
    private PhysicianName physicianName;

    @SerializedName("sections")
    @Expose
    private List<Section> sections;

    @SerializedName("ssn")
    @Expose
    private Ssn ssn;

    @SerializedName("stateAbbreviation")
    @Expose
    private StateAbbreviation stateAbbreviation;

    @SerializedName("streetAddress")
    @Expose
    private StreetAddress streetAddress;

    @SerializedName("trackingNumber")
    @Expose
    private TrackingNumber trackingNumber;

    @SerializedName("url")
    @Expose
    private Url url;

    @SerializedName("vin")
    @Expose
    private Vin vin;

    @SerializedName("zipCode")
    @Expose
    private ZipCode zipCode;

    @SerializedName("medicalCondition")
    @Expose
    private MedicalCondition medicalCondition;

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
     * Determines if a filter exists for a policy.
     * @param filterType The {@link FilterType}.
     * @return <code>true</code> if the policy contains a strategy for the corresponding
     * {@link FilterType}; otherwise <code>false</code>.
     */
    public boolean hasFilter(FilterType filterType) {

        switch(filterType) {

            case CUSTOM_DICTIONARY:
                if(CollectionUtils.isNotEmpty(this.getCustomDictionaries())) { return true; } break;
            case AGE:
                if(this.getAge() != null) { return true; } break;
            case BANK_ROUTING_NUMBER:
                if(this.getBankRoutingNumber() != null) {return true; } break;
            case BITCOIN_ADDRESS:
                if(this.getBitcoinAddress() != null) { return true; } break;
            case CURRENCY:
                if(this.getCurrency() != null) { return true; } break;
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
            case MEDICAL_CONDITION:
                if(this.getMedicalCondition() != null) {return true; } break;
            case PERSON:
                if(this.getPhEye() != null) { return true; } break;
            case PASSPORT_NUMBER:
                if(this.getPassportNumber() != null) { return true; } break;
            case PHONE_NUMBER:
                if(this.getPhoneNumber() != null) { return true; } break;
            case PHONE_NUMBER_EXTENSION:
                if(this.getPhoneNumberExtension() != null) { return true; } break;
            case PHYSICIAN_NAME:
                if(this.getPhysicianName() != null) { return true; } break;
            case SECTION:
                if(this.getSections() != null) { return true; } break;
            case SSN:
                if(this.getSsn() != null) { return true; } break;
            case LOCATION_STATE:
                if(this.getState() != null) { return true; } break;
            case STATE_ABBREVIATION:
                if(this.getStateAbbreviation() != null) { return true; } break;
            case STREET_ADDRESS:
                if(this.getStreetAddress() != null) { return true; } break;
            case SURNAME:
                if(this.getSurname() != null) { return true; } break;
            case TRACKING_NUMBER:
                if(this.getTrackingNumber() != null) { return true; } break;
            case URL:
                if(this.getUrl() != null) { return true; } break;
            case VIN:
                if(this.getVin() != null) { return true; } break;
            case ZIP_CODE:
                if(this.getZipCode() != null) { return true; } break;

        }

        return false;

    }

    public Object getFilter(FilterType filterType) {

        switch(filterType) {

            case CUSTOM_DICTIONARY:
                return this.customDictionaries;
            case AGE:
                return this.age;
            case BANK_ROUTING_NUMBER:
                return this.bankRoutingNumber;
            case BITCOIN_ADDRESS:
                return this.bitcoinAddress;
            case CURRENCY:
                return this.currency;
            case LOCATION_CITY:
                return this.city;
            case LOCATION_COUNTY:
                return this.county;
            case CREDIT_CARD:
                return this.creditCard;
            case DATE:
                return this.date;
            case DRIVERS_LICENSE_NUMBER:
                return this.driversLicense;
            case EMAIL_ADDRESS:
                return this.emailAddress;
            case FIRST_NAME:
                return this.firstName;
            case HOSPITAL:
                return this.hospital;
            case HOSPITAL_ABBREVIATION:
                return this.hospitalAbbreviation;
            case IBAN_CODE:
                return this.ibanCode;
            case IDENTIFIER:
                return this.identifiers;
            case IP_ADDRESS:
                return this.ipAddress;
            case MAC_ADDRESS:
                return this.macAddress;
            case PASSPORT_NUMBER:
                return this.passportNumber;
            case PERSON:
                return this.phEye;
            case PHONE_NUMBER:
                return this.phoneNumber;
            case PHONE_NUMBER_EXTENSION:
                return this.phoneNumberExtension;
            case PHYSICIAN_NAME:
                return this.physicianName;
            case SECTION:
                return this.sections;
            case SSN:
                return this.ssn;
            case LOCATION_STATE:
                return this.state;
            case STATE_ABBREVIATION:
                return this.stateAbbreviation;
            case STREET_ADDRESS:
                return this.streetAddress;
            case SURNAME:
                return this.surname;
            case TRACKING_NUMBER:
                return this.trackingNumber;
            case URL:
                return this.url;
            case VIN:
                return this.vin;
            case ZIP_CODE:
                return this.zipCode;

        }

        // Should never happen.
        return null;

    }

    public void setFilter(FilterType filterType, Object filter) {

        switch(filterType) {

            case CUSTOM_DICTIONARY:
                this.customDictionaries = (List<CustomDictionary>) filter;
                break;
            case AGE:
                this.age = (Age) filter;
                break;
            case BANK_ROUTING_NUMBER:
                this.bankRoutingNumber = (BankRoutingNumber) filter;
                break;
            case BITCOIN_ADDRESS:
                this.bitcoinAddress = (BitcoinAddress) filter;
                break;
            case CURRENCY:
                this.currency = (Currency) filter;
                break;
            case LOCATION_CITY:
                this.city = (City) filter;
                break;
            case LOCATION_COUNTY:
                this.county = (County) filter;
                break;
            case CREDIT_CARD:
                this.creditCard = (CreditCard) filter;
                break;
            case DATE:
                this.date = (Date) filter;
                break;
            case DRIVERS_LICENSE_NUMBER:
                this.driversLicense = (DriversLicense) filter;
                break;
            case EMAIL_ADDRESS:
                this.emailAddress = (EmailAddress) filter;
                break;
            case FIRST_NAME:
                this.firstName = (FirstName) filter;
                break;
            case HOSPITAL:
                this.hospital = (Hospital) filter;
                break;
            case HOSPITAL_ABBREVIATION:
                this.hospitalAbbreviation = (HospitalAbbreviation) filter;
                break;
            case IBAN_CODE:
                this.ibanCode = (IbanCode) filter;
                break;
            case IDENTIFIER:
                this.identifiers = (List<Identifier>) filter;
                break;
            case IP_ADDRESS:
                this.ipAddress = (IpAddress) filter;
                break;
            case MAC_ADDRESS:
                this.macAddress = (MacAddress) filter;
                break;
            case PASSPORT_NUMBER:
                this.passportNumber = (PassportNumber) filter;
                break;
            case PERSON:
                this.phEye = (PhEye) filter;
                break;
            case PHONE_NUMBER:
                this.phoneNumber = (PhoneNumber) filter;
                break;
            case PHONE_NUMBER_EXTENSION:
                this.phoneNumberExtension = (PhoneNumberExtension) filter;
                break;
            case PHYSICIAN_NAME:
                this.physicianName = (PhysicianName) filter;
                break;
            case SECTION:
                this.sections = (List<Section>) filter;
                break;
            case SSN:
                this.ssn = (Ssn) filter;
                break;
            case LOCATION_STATE:
                this.state = (State) filter;
                break;
            case STATE_ABBREVIATION:
                this.stateAbbreviation = (StateAbbreviation) filter;
                break;
            case STREET_ADDRESS:
                this.streetAddress = (StreetAddress) filter;
                break;
            case SURNAME:
                this.surname = (Surname) filter;
                break;
            case TRACKING_NUMBER:
                this.trackingNumber = (TrackingNumber) filter;
                break;
            case URL:
                this.url = (Url) filter;
                break;
            case VIN:
                this.vin = (Vin) filter;
                break;
            case ZIP_CODE:
                this.zipCode = (ZipCode) filter;
                break;

        }

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

    public PhEye getPhEye() {
        return phEye;
    }

    public void setPerson(PhEye phEye) {
        this.phEye = phEye;
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

    public TrackingNumber getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(TrackingNumber trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public StreetAddress getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(StreetAddress streetAddress) {
        this.streetAddress = streetAddress;
    }

    public PhysicianName getPhysicianName() {
        return physicianName;
    }

    public void setPhysicianName(PhysicianName physicianName) {
        this.physicianName = physicianName;
    }

    public BankRoutingNumber getBankRoutingNumber() {
        return bankRoutingNumber;
    }

    public void setBankRoutingNumber(BankRoutingNumber bankRoutingNumber) {
        this.bankRoutingNumber = bankRoutingNumber;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public MedicalCondition getMedicalCondition() {
        return medicalCondition;
    }

    public void setMedicalCondition(MedicalCondition medicalCondition) {
        this.medicalCondition = medicalCondition;
    }

}
