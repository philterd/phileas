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
package ai.philterd.phileas.services;

import ai.philterd.phileas.policy.Config;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Ignored;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.Splitting;
import ai.philterd.phileas.policy.filters.Age;
import ai.philterd.phileas.policy.filters.City;
import ai.philterd.phileas.policy.filters.County;
import ai.philterd.phileas.policy.filters.CreditCard;
import ai.philterd.phileas.policy.filters.CustomDictionary;
import ai.philterd.phileas.policy.filters.Date;
import ai.philterd.phileas.policy.filters.EmailAddress;
import ai.philterd.phileas.policy.filters.FirstName;
import ai.philterd.phileas.policy.filters.Hospital;
import ai.philterd.phileas.policy.filters.Identifier;
import ai.philterd.phileas.policy.filters.IpAddress;
import ai.philterd.phileas.policy.filters.PhoneNumber;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.policy.filters.State;
import ai.philterd.phileas.policy.filters.StateAbbreviation;
import ai.philterd.phileas.policy.filters.StreetAddress;
import ai.philterd.phileas.policy.filters.Surname;
import ai.philterd.phileas.policy.filters.Url;
import ai.philterd.phileas.policy.filters.Vin;
import ai.philterd.phileas.policy.filters.ZipCode;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.custom.CustomDictionaryFilterStrategy;
import ai.philterd.phileas.services.strategies.dynamic.CityFilterStrategy;
import ai.philterd.phileas.services.strategies.dynamic.CountyFilterStrategy;
import ai.philterd.phileas.services.strategies.dynamic.FirstNameFilterStrategy;
import ai.philterd.phileas.services.strategies.dynamic.HospitalFilterStrategy;
import ai.philterd.phileas.services.strategies.dynamic.StateFilterStrategy;
import ai.philterd.phileas.services.strategies.dynamic.SurnameFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.AgeFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.CreditCardFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.DateFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.IdentifierFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.IpAddressFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.PhoneNumberFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.StateAbbreviationFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.StreetAddressFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.UrlFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.VinFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.ZipCodeFilterStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EndToEndTestsHelper {

    private static final Logger LOGGER = LogManager.getLogger(EndToEndTestsHelper.class);

    public static Policy getPolicyZipCodeWithIgnored() throws IOException {

        Set<String> ignored = new HashSet<>();
        ignored.add("90210");

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(ssnFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(List.of(zipCodeFilterStrategy));
        zipCode.setIgnored(ignored);

        Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);
        identifiers.setZipCode(zipCode);

        Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return policy;

    }

    public static Policy getPolicySSNAndZipCode() throws IOException {

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(ssnFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(List.of(zipCodeFilterStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);
        identifiers.setZipCode(zipCode);

        Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return policy;

    }

    public static Policy getPolicyZipCodeWithIgnoredFromFile() throws IOException {

        // Copy file to temp directory.
        final File file = File.createTempFile("philter", "ignore");
        FileUtils.writeLines(file, Arrays.asList("90210", "John Smith"));

        Set<String> ignoredFiles = new HashSet<>();
        ignoredFiles.add(file.getAbsolutePath());

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(ssnFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(List.of(zipCodeFilterStrategy));
        zipCode.setIgnoredFiles(ignoredFiles);

        Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);
        identifiers.setZipCode(zipCode);

        Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return policy;

    }

    public static Policy getPdfPolicy() throws IOException {

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(List.of(zipCodeFilterStrategy));

        CustomDictionaryFilterStrategy customDictionaryFilterStrategy = new CustomDictionaryFilterStrategy();
        customDictionaryFilterStrategy.setStrategy("REDACT");

        CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setCustomDictionaryFilterStrategies(List.of(customDictionaryFilterStrategy));
        customDictionary.setTerms(List.of("Wendy"));

        Identifiers identifiers = new Identifiers();

        identifiers.setCustomDictionaries(List.of(customDictionary));
        identifiers.setZipCode(zipCode);

        Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return policy;

    }

    public static Policy getPolicyJustCreditCardNotInUnixTimestamps() {

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();

        CreditCard creditCard = new CreditCard();
        creditCard.setIgnoreWhenInUnixTimestamp(true);
        creditCard.setCreditCardFilterStrategies(List.of(creditCardFilterStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setCreditCard(creditCard);

        Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("4121742025464400", "12341341234", "2423543545"));

        Policy policy = new Policy();
        policy.setIdentifiers(identifiers);
        policy.setIgnored(List.of(ignored));

        return policy;

    }

    public static Policy getPolicyJustCreditCard() {

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(List.of(creditCardFilterStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setCreditCard(creditCard);

        Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("4121742025464400", "12341341234", "2423543545"));

        Policy policy = new Policy();
        policy.setIdentifiers(identifiers);
        policy.setIgnored(List.of(ignored));

        return policy;

    }

    public static Policy getPolicy() throws IOException, URISyntaxException {

        AgeFilterStrategy ageFilterStrategy = new AgeFilterStrategy();

        Age age = new Age();
        age.setAgeFilterStrategies(List.of(ageFilterStrategy));

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(List.of(creditCardFilterStrategy));

        DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        dateFilterStrategy.setStrategy(AbstractFilterStrategy.SHIFT);
        dateFilterStrategy.setShiftYears(3);
        dateFilterStrategy.setShiftMonths(2);
        dateFilterStrategy.setShiftDays(1);

        Date date = new Date();
        date.setDateFilterStrategies(List.of(dateFilterStrategy));

        EmailAddressFilterStrategy emailAddressFilterStrategy = new EmailAddressFilterStrategy();

        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(List.of(emailAddressFilterStrategy));

        Identifier identifier1 = new Identifier();
        identifier1.setIdentifierFilterStrategies(List.of(new IdentifierFilterStrategy()));
        identifier1.setPattern("asdfasdfasdf");
        identifier1.setCaseSensitive(true);

        IdentifierFilterStrategy identifier2FilterStrategy = new IdentifierFilterStrategy();
        identifier2FilterStrategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        identifier2FilterStrategy.setStaticReplacement("STATIC-REPLACEMENT");
        Identifier identifier2 = new Identifier();
        identifier2.setPattern("JEFF");
        identifier2.setCaseSensitive(true);
        identifier2.setIdentifierFilterStrategies(List.of(identifier2FilterStrategy));

        IpAddressFilterStrategy ipAddressFilterStrategy = new IpAddressFilterStrategy();

        IpAddress ipAddress = new IpAddress();
        ipAddress.setIpAddressFilterStrategies(List.of(ipAddressFilterStrategy));

        PhoneNumberFilterStrategy phoneNumberFilterStrategy = new PhoneNumberFilterStrategy();

        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPhoneNumberFilterStrategies(List.of(phoneNumberFilterStrategy));

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(ssnFilterStrategy));

        StateAbbreviationFilterStrategy stateAbbreviationFilterStrategy = new StateAbbreviationFilterStrategy();

        StateAbbreviation stateAbbreviation = new StateAbbreviation();
        stateAbbreviation.setStateAbbreviationsFilterStrategies(List.of(stateAbbreviationFilterStrategy));

        UrlFilterStrategy urlFilterStrategy = new UrlFilterStrategy();

        Url url = new Url();
        url.setUrlFilterStrategies(List.of(urlFilterStrategy));

        VinFilterStrategy vinFilterStrategy = new VinFilterStrategy();

        Vin vin = new Vin();
        vin.setVinFilterStrategies(List.of(vinFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(List.of(zipCodeFilterStrategy));

        CityFilterStrategy cityFilterStrategy = new CityFilterStrategy();

        City city = new City();
        city.setCityFilterStrategies(List.of(cityFilterStrategy));

        CountyFilterStrategy countyFilterStrategy = new CountyFilterStrategy();

        County county = new County();
        county.setCountyFilterStrategies(List.of(countyFilterStrategy));

        FirstNameFilterStrategy firstNameFilterStrategy = new FirstNameFilterStrategy();

        FirstName firstName = new FirstName();
        firstName.setFirstNameFilterStrategies(List.of(firstNameFilterStrategy));

        HospitalFilterStrategy hospitalFilterStrategy = new HospitalFilterStrategy();

        Hospital hospital = new Hospital();
        hospital.setHospitalFilterStrategies(List.of(hospitalFilterStrategy));

        StateFilterStrategy stateFilterStrategy = new StateFilterStrategy();

        State state = new State();
        state.setStateFilterStrategies(List.of(stateFilterStrategy));

        SurnameFilterStrategy surnameFilterStrategy = new SurnameFilterStrategy();

        Surname surname = new Surname();
        surname.setSurnameFilterStrategies(List.of(surnameFilterStrategy));

        // ----------------------------------------------------------------------------------

        Identifiers identifiers = new Identifiers();

        identifiers.setAge(age);
        identifiers.setCreditCard(creditCard);
        identifiers.setDate(date);
        identifiers.setEmailAddress(emailAddress);
        identifiers.setIdentifiers(Arrays.asList(identifier1, identifier2));
        identifiers.setIpAddress(ipAddress);
        //identifiers.setPersonV2(personV2);
        identifiers.setPhoneNumber(phoneNumber);
        identifiers.setSsn(ssn);
        //identifiers.setStateAbbreviation(stateAbbreviation);
        identifiers.setUrl(url);
        identifiers.setVin(vin);
        identifiers.setZipCode(zipCode);

        /*identifiers.setCity(city);
        identifiers.setCounty(county);
        identifiers.setFirstName(firstName);
        identifiers.setHospital(hospital);
        identifiers.setHospitalAbbreviation(hospitalAbbreviation);
        identifiers.setState(state);
        identifiers.setSurname(surname);*/

        Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return policy;

    }

    public static Policy getPolicyWithSplits() throws IOException {

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(ssnFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(List.of(zipCodeFilterStrategy));

        Identifiers identifiers = new Identifiers();

        identifiers.setSsn(ssn);
        identifiers.setZipCode(zipCode);

        Splitting splitting = new Splitting();
        splitting.setThreshold(15);
        splitting.setEnabled(true);

        Config config = new Config();
        config.setSplitting(splitting);

        Policy policy = new Policy();
        policy.setIdentifiers(identifiers);
        policy.setConfig(config);

        return policy;

    }

    public static Policy getPolicyJustStreetAddress() {

        StreetAddressFilterStrategy streetAddressFilterStrategy = new StreetAddressFilterStrategy();

        StreetAddress streetAddress = new StreetAddress();
        streetAddress.setStreetAddressFilterStrategies(List.of(streetAddressFilterStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setStreetAddress(streetAddress);

        Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return policy;

    }

    public static Policy getPolicyJustPhoneNumber() {

        PhoneNumberFilterStrategy phoneNumberFilterStrategy = new PhoneNumberFilterStrategy();
        phoneNumberFilterStrategy.setConditions("confidence > 0.70");

        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPhoneNumberFilterStrategies(List.of(phoneNumberFilterStrategy));
        phoneNumber.setIgnored(Set.of("102-304-5678"));

        Identifiers identifiers = new Identifiers();
        identifiers.setPhoneNumber(phoneNumber);

        Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return policy;

    }

    public static boolean documentContainsText(byte[] doc, String needle) throws IOException {
        try (PDDocument pdDocument = Loader.loadPDF(doc)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String pdfText = textStripper.getText(pdDocument);

            if(pdfText.trim().isEmpty()) {
                LOGGER.warn("documentContainsText called on a PDF with no text streams");
            }

            return pdfText.contains(needle);
        }
    }
}
