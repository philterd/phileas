package ai.philterd.test.phileas.services;

import ai.philterd.phileas.model.profile.FilterProfile;
import ai.philterd.phileas.model.profile.Identifiers;
import ai.philterd.phileas.model.profile.Ignored;
import ai.philterd.phileas.model.profile.filters.Age;
import ai.philterd.phileas.model.profile.filters.City;
import ai.philterd.phileas.model.profile.filters.County;
import ai.philterd.phileas.model.profile.filters.CreditCard;
import ai.philterd.phileas.model.profile.filters.CustomDictionary;
import ai.philterd.phileas.model.profile.filters.Date;
import ai.philterd.phileas.model.profile.filters.EmailAddress;
import ai.philterd.phileas.model.profile.filters.FirstName;
import ai.philterd.phileas.model.profile.filters.Hospital;
import ai.philterd.phileas.model.profile.filters.HospitalAbbreviation;
import ai.philterd.phileas.model.profile.filters.Identifier;
import ai.philterd.phileas.model.profile.filters.IpAddress;
import ai.philterd.phileas.model.profile.filters.Person;
import ai.philterd.phileas.model.profile.filters.PhoneNumber;
import ai.philterd.phileas.model.profile.filters.Ssn;
import ai.philterd.phileas.model.profile.filters.State;
import ai.philterd.phileas.model.profile.filters.StateAbbreviation;
import ai.philterd.phileas.model.profile.filters.Surname;
import ai.philterd.phileas.model.profile.filters.Url;
import ai.philterd.phileas.model.profile.filters.Vin;
import ai.philterd.phileas.model.profile.filters.ZipCode;
import ai.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.ai.PersonsFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.dynamic.CityFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.dynamic.CountyFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.dynamic.FirstNameFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.dynamic.HospitalAbbreviationFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.dynamic.HospitalFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.dynamic.StateFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.dynamic.SurnameFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.AgeFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.CreditCardFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.DateFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.IdentifierFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.IpAddressFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.PhoneNumberFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.SsnFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.StateAbbreviationFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.UrlFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.VinFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.ZipCodeFilterStrategy;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EndToEndTestsHelper {


    public static FilterProfile getFilterProfileZipCodeWithIgnored(String filterProfileName) throws IOException {

        Set<String> ignored = new HashSet<>();
        ignored.add("90210");

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(Arrays.asList(ssnFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));
        zipCode.setIgnored(ignored);

        Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);
        identifiers.setZipCode(zipCode);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    public static FilterProfile getFilterProfileZipCodeWithIgnoredFromFile(String filterProfileName) throws IOException {

        // Copy file to temp directory.
        final File file = File.createTempFile("philter", "ignore");
        FileUtils.writeLines(file, Arrays.asList("90210", "John Smith"));

        Set<String> ignoredFiles = new HashSet<>();
        ignoredFiles.add(file.getAbsolutePath());

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(Arrays.asList(ssnFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));
        zipCode.setIgnoredFiles(ignoredFiles);

        Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);
        identifiers.setZipCode(zipCode);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    public static FilterProfile getPdfFilterProfile(String filterProfileName) throws IOException {

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));

        CustomDictionaryFilterStrategy customDictionaryFilterStrategy = new CustomDictionaryFilterStrategy();
        customDictionaryFilterStrategy.setStrategy("REDACT");

        CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setCustomDictionaryFilterStrategies(Arrays.asList(customDictionaryFilterStrategy));
        customDictionary.setTerms(Arrays.asList("Wendy"));

        Identifiers identifiers = new Identifiers();

        identifiers.setCustomDictionaries(Arrays.asList(customDictionary));
        identifiers.setZipCode(zipCode);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    public static FilterProfile getPdfFilterWithPersonProfile(String filterProfileName) throws URISyntaxException {

        final File model = new File(EndToEndTestsHelper.class.getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(EndToEndTestsHelper.class.getClassLoader().getResource("ner/vocab.txt").toURI());

        final Person person = new Person();
        person.setModel(model.getAbsolutePath());
        person.setVocab(vocab.getAbsolutePath());

        Identifiers identifiers = new Identifiers();
        identifiers.setPerson(person);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    public static FilterProfile getFilterProfileJustCreditCard(String filterProfileName) throws IOException {

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(Arrays.asList(creditCardFilterStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setCreditCard(creditCard);

        Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("4121742025464400", "12341341234", "2423543545"));

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);
        filterProfile.setIgnored(Arrays.asList(ignored));

        return filterProfile;

    }

    public static FilterProfile getFilterProfile(String filterProfileName) throws IOException, URISyntaxException {

        AgeFilterStrategy ageFilterStrategy = new AgeFilterStrategy();

        Age age = new Age();
        age.setAgeFilterStrategies(Arrays.asList(ageFilterStrategy));

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(Arrays.asList(creditCardFilterStrategy));

        DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        dateFilterStrategy.setStrategy(AbstractFilterStrategy.SHIFT);
        dateFilterStrategy.setShiftYears(3);
        dateFilterStrategy.setShiftMonths(2);
        dateFilterStrategy.setShiftDays(1);

        Date date = new Date();
        date.setDateFilterStrategies(Arrays.asList(dateFilterStrategy));

        EmailAddressFilterStrategy emailAddressFilterStrategy = new EmailAddressFilterStrategy();

        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(Arrays.asList(emailAddressFilterStrategy));

        Identifier identifier1 = new Identifier();
        identifier1.setIdentifierFilterStrategies(Arrays.asList(new IdentifierFilterStrategy()));
        identifier1.setPattern("asdfasdfasdf");
        identifier1.setCaseSensitive(true);

        IdentifierFilterStrategy identifier2FilterStrategy = new IdentifierFilterStrategy();
        identifier2FilterStrategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        identifier2FilterStrategy.setStaticReplacement("STATIC-REPLACEMENT");
        Identifier identifier2 = new Identifier();
        identifier2.setPattern("JEFF");
        identifier2.setCaseSensitive(true);
        identifier2.setIdentifierFilterStrategies(Arrays.asList(identifier2FilterStrategy));

        IpAddressFilterStrategy ipAddressFilterStrategy = new IpAddressFilterStrategy();

        IpAddress ipAddress = new IpAddress();
        ipAddress.setIpAddressFilterStrategies(Arrays.asList(ipAddressFilterStrategy));

        PhoneNumberFilterStrategy phoneNumberFilterStrategy = new PhoneNumberFilterStrategy();

        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPhoneNumberFilterStrategies(Arrays.asList(phoneNumberFilterStrategy));

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(Arrays.asList(ssnFilterStrategy));

        StateAbbreviationFilterStrategy stateAbbreviationFilterStrategy = new StateAbbreviationFilterStrategy();

        StateAbbreviation stateAbbreviation = new StateAbbreviation();
        stateAbbreviation.setStateAbbreviationsFilterStrategies(Arrays.asList(stateAbbreviationFilterStrategy));

        UrlFilterStrategy urlFilterStrategy = new UrlFilterStrategy();

        Url url = new Url();
        url.setUrlFilterStrategies(Arrays.asList(urlFilterStrategy));

        VinFilterStrategy vinFilterStrategy = new VinFilterStrategy();

        Vin vin = new Vin();
        vin.setVinFilterStrategies(Arrays.asList(vinFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));

        PersonsFilterStrategy personsFilterStrategy = new PersonsFilterStrategy();

        final File model = new File(EndToEndTestsHelper.class.getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(EndToEndTestsHelper.class.getClassLoader().getResource("ner/vocab.txt").toURI());

        Person person = new Person();
        person.setModel(model.getAbsolutePath());
        person.setVocab(vocab.getAbsolutePath());
        person.setPersonFilterStrategies(Arrays.asList(personsFilterStrategy));

        // ----------------------------------------------------------------------------------

        CityFilterStrategy cityFilterStrategy = new CityFilterStrategy();

        City city = new City();
        city.setCityFilterStrategies(Arrays.asList(cityFilterStrategy));

        CountyFilterStrategy countyFilterStrategy = new CountyFilterStrategy();

        County county = new County();
        county.setCountyFilterStrategies(Arrays.asList(countyFilterStrategy));

        FirstNameFilterStrategy firstNameFilterStrategy = new FirstNameFilterStrategy();

        FirstName firstName = new FirstName();
        firstName.setFirstNameFilterStrategies(Arrays.asList(firstNameFilterStrategy));

        HospitalAbbreviationFilterStrategy hospitalAbbreviationFilterStrategy = new HospitalAbbreviationFilterStrategy();

        HospitalAbbreviation hospitalAbbreviation = new HospitalAbbreviation();
        hospitalAbbreviation.setHospitalAbbreviationFilterStrategies(Arrays.asList(hospitalAbbreviationFilterStrategy));

        HospitalFilterStrategy hospitalFilterStrategy = new HospitalFilterStrategy();

        Hospital hospital = new Hospital();
        hospital.setHospitalFilterStrategies(Arrays.asList(hospitalFilterStrategy));

        StateFilterStrategy stateFilterStrategy = new StateFilterStrategy();

        State state = new State();
        state.setStateFilterStrategies(Arrays.asList(stateFilterStrategy));

        SurnameFilterStrategy surnameFilterStrategy = new SurnameFilterStrategy();

        Surname surname = new Surname();
        surname.setSurnameFilterStrategies(Arrays.asList(surnameFilterStrategy));

        // ----------------------------------------------------------------------------------

        Identifiers identifiers = new Identifiers();

        identifiers.setAge(age);
        identifiers.setCreditCard(creditCard);
        identifiers.setDate(date);
        identifiers.setEmailAddress(emailAddress);
        identifiers.setIdentifiers(Arrays.asList(identifier1, identifier2));
        identifiers.setIpAddress(ipAddress);
        identifiers.setPerson(person);
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

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    public static FilterProfile getFilterProfileJustIdentifier(String filterProfileName) throws IOException, URISyntaxException {

        Identifier identifier1 = new Identifier();
        identifier1.setIdentifierFilterStrategies(Arrays.asList(new IdentifierFilterStrategy()));
        identifier1.setPattern("\\b\\d{3,8}\\b");
        identifier1.setCaseSensitive(false);

        Identifiers identifiers = new Identifiers();

        identifiers.setIdentifiers(Arrays.asList(identifier1));

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

}
