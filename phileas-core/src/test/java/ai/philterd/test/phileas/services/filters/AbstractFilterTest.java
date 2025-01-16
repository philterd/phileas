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
package ai.philterd.test.phileas.services.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Identifiers;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.filters.Age;
import ai.philterd.phileas.model.policy.filters.BankRoutingNumber;
import ai.philterd.phileas.model.policy.filters.BitcoinAddress;
import ai.philterd.phileas.model.policy.filters.City;
import ai.philterd.phileas.model.policy.filters.County;
import ai.philterd.phileas.model.policy.filters.CreditCard;
import ai.philterd.phileas.model.policy.filters.Currency;
import ai.philterd.phileas.model.policy.filters.CustomDictionary;
import ai.philterd.phileas.model.policy.filters.Date;
import ai.philterd.phileas.model.policy.filters.DriversLicense;
import ai.philterd.phileas.model.policy.filters.EmailAddress;
import ai.philterd.phileas.model.policy.filters.FirstName;
import ai.philterd.phileas.model.policy.filters.Hospital;
import ai.philterd.phileas.model.policy.filters.HospitalAbbreviation;
import ai.philterd.phileas.model.policy.filters.IbanCode;
import ai.philterd.phileas.model.policy.filters.Identifier;
import ai.philterd.phileas.model.policy.filters.IpAddress;
import ai.philterd.phileas.model.policy.filters.MacAddress;
import ai.philterd.phileas.model.policy.filters.PassportNumber;
import ai.philterd.phileas.model.policy.filters.PhoneNumber;
import ai.philterd.phileas.model.policy.filters.PhoneNumberExtension;
import ai.philterd.phileas.model.policy.filters.PhysicianName;
import ai.philterd.phileas.model.policy.filters.Section;
import ai.philterd.phileas.model.policy.filters.Ssn;
import ai.philterd.phileas.model.policy.filters.State;
import ai.philterd.phileas.model.policy.filters.StateAbbreviation;
import ai.philterd.phileas.model.policy.filters.StreetAddress;
import ai.philterd.phileas.model.policy.filters.Surname;
import ai.philterd.phileas.model.policy.filters.TrackingNumber;
import ai.philterd.phileas.model.policy.filters.Url;
import ai.philterd.phileas.model.policy.filters.Vin;
import ai.philterd.phileas.model.policy.filters.ZipCode;
import ai.philterd.phileas.model.policy.filters.strategies.custom.CustomDictionaryFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.CityFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.CountyFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.FirstNameFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.HospitalAbbreviationFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.HospitalFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.StateFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.SurnameFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.AgeFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.BankRoutingNumberFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.BitcoinAddressFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.CreditCardFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.CurrencyFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.DateFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.DriversLicenseFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.IbanCodeFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.IdentifierFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.IpAddressFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.MacAddressFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.PassportNumberFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.PhoneNumberExtensionFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.PhoneNumberFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.PhysicianNameFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.SectionFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.SsnFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.StateAbbreviationFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.StreetAddressFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.TrackingNumberFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.UrlFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.VinFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.ZipCodeFilterStrategy;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractFilterTest {

    protected static final Logger LOGGER = LogManager.getLogger(AbstractFilterTest.class);

    protected static final int PIECE = 0;
    protected final int windowSize = 3;
    protected final Map<String, String> attributes = new HashMap<>();

    /**
     * Gets a {@link Policy} where all non-deterministic filters use the given {@link SensitivityLevel}.
     * @return A {@link Policy} where all non-deterministic filters use the given {@link SensitivityLevel}.
     */
    public Policy getPolicy() throws IOException {

        CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setCustomDictionaryFilterStrategies(Arrays.asList(new CustomDictionaryFilterStrategy()));

        Age age = new Age();
        age.setAgeFilterStrategies(Arrays.asList(new AgeFilterStrategy()));

        BankRoutingNumber bankRoutingNumber = new BankRoutingNumber();
        bankRoutingNumber.setBankRoutingNumberFilterStrategies(Arrays.asList(new BankRoutingNumberFilterStrategy()));

        BitcoinAddress bitcoinAddress = new BitcoinAddress();
        bitcoinAddress.setBitcoinFilterStrategies(Arrays.asList(new BitcoinAddressFilterStrategy()));

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(Arrays.asList(new CreditCardFilterStrategy()));
        creditCard.setOnlyValidCreditCardNumbers(true);

        Currency currency = new Currency();
        currency.setCurrencyFilterStrategies(Arrays.asList(new CurrencyFilterStrategy()));

        Date date = new Date();
        date.setDateFilterStrategies(Arrays.asList(new DateFilterStrategy()));

        DriversLicense driversLicense = new DriversLicense();
        driversLicense.setDriversLicenseFilterStrategies(Arrays.asList(new DriversLicenseFilterStrategy()));

        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(Arrays.asList(new EmailAddressFilterStrategy()));

        IbanCode ibanCode = new IbanCode();
        ibanCode.setIbanCodeFilterStrategies(Arrays.asList(new IbanCodeFilterStrategy()));

        Identifier identifier = new Identifier();
        identifier.setIdentifierFilterStrategies(Arrays.asList(new IdentifierFilterStrategy()));

        IpAddress ipAddress = new IpAddress();
        ipAddress.setIpAddressFilterStrategies(Arrays.asList(new IpAddressFilterStrategy()));

        MacAddress macAddress = new MacAddress();
        macAddress.setMacAddressFilterStrategies(Arrays.asList(new MacAddressFilterStrategy()));

        PassportNumber passportNumber = new PassportNumber();
        passportNumber.setPassportNumberFilterStrategies(Arrays.asList(new PassportNumberFilterStrategy()));

        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPhoneNumberFilterStrategies(Arrays.asList(new PhoneNumberFilterStrategy()));

        PhoneNumberExtension phoneNumberExtension = new PhoneNumberExtension();
        phoneNumberExtension.setPhoneNumberExtensionFilterStrategies(Arrays.asList(new PhoneNumberExtensionFilterStrategy()));

        PhysicianName physicianName = new PhysicianName();
        physicianName.setPhysicianNameFilterStrategies(Arrays.asList(new PhysicianNameFilterStrategy()));

        Section section = new Section();
        section.setSectionFilterStrategies(Arrays.asList(new SectionFilterStrategy()));

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(Arrays.asList(new SsnFilterStrategy()));

        StateAbbreviation stateAbbreviation = new StateAbbreviation();
        stateAbbreviation.setStateAbbreviationsFilterStrategies(Arrays.asList(new StateAbbreviationFilterStrategy()));

        StreetAddress streetAddress = new StreetAddress();
        streetAddress.setStreetAddressFilterStrategies(Arrays.asList(new StreetAddressFilterStrategy()));

        TrackingNumber trackingNumber = new TrackingNumber();
        trackingNumber.setTrackingNumberFilterStrategies(Arrays.asList(new TrackingNumberFilterStrategy()));

        Url url = new Url();
        url.setUrlFilterStrategies(Arrays.asList(new UrlFilterStrategy()));

        Vin vin = new Vin();
        vin.setVinFilterStrategies(Arrays.asList(new VinFilterStrategy()));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));

        // ----------------------------------------------------------------------------------

        City city = new City();
        city.setCityFilterStrategies(Arrays.asList(new CityFilterStrategy()));

        County county = new County();
        county.setCountyFilterStrategies(Arrays.asList(new CountyFilterStrategy()));

        FirstNameFilterStrategy firstNameFilterStrategy = new FirstNameFilterStrategy();

        FirstName firstName = new FirstName();
        firstName.setFirstNameFilterStrategies(Arrays.asList(firstNameFilterStrategy));

        HospitalAbbreviation hospitalAbbreviation = new HospitalAbbreviation();
        hospitalAbbreviation.setHospitalAbbreviationFilterStrategies(Arrays.asList(new HospitalAbbreviationFilterStrategy()));

        HospitalFilterStrategy hospitalFilterStrategy = new HospitalFilterStrategy();

        Hospital hospital = new Hospital();
        hospital.setHospitalFilterStrategies(Arrays.asList(hospitalFilterStrategy));

        State state = new State();
        state.setStateFilterStrategies(Arrays.asList(new StateFilterStrategy()));

        Surname surname = new Surname();
        surname.setSurnameFilterStrategies(Arrays.asList(new SurnameFilterStrategy()));

        // ----------------------------------------------------------------------------------

        Identifiers identifiers = new Identifiers();

        identifiers.setCustomDictionaries(Arrays.asList(customDictionary));

        identifiers.setAge(age);
        identifiers.setBankRoutingNumber(bankRoutingNumber);
        identifiers.setBitcoinAddress(bitcoinAddress);
        identifiers.setCreditCard(creditCard);
        identifiers.setCurrency(currency);
        identifiers.setDate(date);
        identifiers.setDriversLicense(driversLicense);
        identifiers.setEmailAddress(emailAddress);
        identifiers.setIdentifiers(Arrays.asList(identifier));
        identifiers.setIbanCode(ibanCode);
        identifiers.setIpAddress(ipAddress);
        identifiers.setMacAddress(macAddress);
        identifiers.setPassportNumber(passportNumber);
        identifiers.setPhoneNumber(phoneNumber);
        identifiers.setPhoneNumberExtension(phoneNumberExtension);
        identifiers.setPhysicianName(physicianName);
        identifiers.setSections(Arrays.asList(section));
        identifiers.setSsn(ssn);
        identifiers.setStateAbbreviation(stateAbbreviation);
        identifiers.setStreetAddress(streetAddress);
        identifiers.setTrackingNumber(trackingNumber);
        identifiers.setUrl(url);
        identifiers.setVin(vin);
        identifiers.setZipCode(zipCode);

        identifiers.setCity(city);
        identifiers.setCounty(county);
        identifiers.setFirstName(firstName);
        identifiers.setHospital(hospital);
        identifiers.setHospitalAbbreviation(hospitalAbbreviation);
        identifiers.setState(state);
        identifiers.setSurname(surname);

        Policy policy = new Policy();
        policy.setName("default");
        policy.setIdentifiers(identifiers);

        return policy;

    }

    public void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

    public boolean checkSpan(Span span, int characterStart, int characterEnd, FilterType filterType) {

        LOGGER.info("Checking span: {}", span.toString());

        return (span.getCharacterStart() == characterStart
                && span.getCharacterEnd() == characterEnd
                && span.getFilterType() == filterType);

    }

    public boolean checkSpanInSpans(List<Span> spans, int characterStart, int characterEnd,
                                    FilterType filterType, String text, String replacement) {

        for(final Span span : spans) {

            if (span.getCharacterStart() == characterStart
                    && span.getCharacterEnd() == characterEnd
                    && span.getFilterType() == filterType
                    && StringUtils.equals(text, span.getText())
                    && StringUtils.equals(replacement, span.getReplacement()) == true) {

                return true;

            }

        }

        // None of the spans in the list match this one.
        return false;

    }

}
