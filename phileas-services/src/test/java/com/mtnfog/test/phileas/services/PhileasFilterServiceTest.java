package com.mtnfog.test.phileas.services;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.exceptions.InvalidFilterProfileException;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Identifiers;
import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.model.profile.filters.*;
import com.mtnfog.phileas.model.profile.filters.Date;
import com.mtnfog.phileas.model.profile.filters.strategies.ai.NerFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.*;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.*;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import com.mtnfog.phileas.model.services.FilterProfileService;
import com.mtnfog.phileas.services.PhileasFilterService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.registry.LocalFilterProfileService;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PhileasFilterServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(PhileasFilterServiceTest.class);

    private String INDEXES_DIRECTORY = "/mtnfog/code/bitbucket/philter/philter/distribution/indexes/";
    private Gson gson = new Gson();

    @Before
    public void before() {
        INDEXES_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEXES_DIRECTORY.substring(1) : INDEXES_DIRECTORY;
    }

    @Test
    public void filterProfile() throws IOException {

        final FilterProfile filterProfile = getFilterProfile("default");
        LOGGER.info(gson.toJson(filterProfile));

    }

    @Test
    public void endToEnd1() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "profile.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties applicationProperties = new Properties();
        applicationProperties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        applicationProperties.setProperty("store.enabled", "false");
        applicationProperties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        AnonymizationCacheService anonymizationCacheService = new LocalAnonymizationCacheService();
        LocalFilterProfileService filterProfileService = new LocalFilterProfileService(applicationProperties);
        List<FilterProfileService> filterProfileServices = Arrays.asList(filterProfileService);

        PhileasFilterService service = new PhileasFilterService(applicationProperties, filterProfileServices, anonymizationCacheService, "http://localhost:18080/");
        final FilterResponse response = service.filter("default", "context", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.");

        LOGGER.info(response.getFilteredText());

        Assert.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at {{{REDACTED-zip-code}}}.", response.getFilteredText());

    }

    @Test
    public void endToEnd2() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "profile.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties applicationProperties = new Properties();
        applicationProperties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        applicationProperties.setProperty("store.enabled", "false");
        applicationProperties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        AnonymizationCacheService anonymizationCacheService = new LocalAnonymizationCacheService();
        LocalFilterProfileService filterProfileService = new LocalFilterProfileService(applicationProperties);
        List<FilterProfileService> filterProfileServices = Arrays.asList(filterProfileService);

        PhileasFilterService service = new PhileasFilterService(applicationProperties, filterProfileServices, anonymizationCacheService, "http://localhost:18080/");
        final FilterResponse response = service.filter("default", "context", "My email is test@something.com and cc is 4121742025464465");

        LOGGER.info(response.getFilteredText());

        Assert.assertEquals("My email is {{{REDACTED-email-address}}} and cc is {{{REDACTED-credit-card}}}", response.getFilteredText());

    }

    @Test
    public void endToEnd3() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "profile.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties applicationProperties = new Properties();
        applicationProperties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        applicationProperties.setProperty("store.enabled", "false");
        applicationProperties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        AnonymizationCacheService anonymizationCacheService = new LocalAnonymizationCacheService();
        LocalFilterProfileService filterProfileService = new LocalFilterProfileService(applicationProperties);
        List<FilterProfileService> filterProfileServices = Arrays.asList(filterProfileService);

        PhileasFilterService service = new PhileasFilterService(applicationProperties, filterProfileServices, anonymizationCacheService, "http://localhost:18080/");
        final FilterResponse response = service.filter("default", "context", "test@something.com is email and cc is 4121742025464465");

        LOGGER.info(response.getFilteredText());

        Assert.assertEquals("{{{REDACTED-email-address}}} is email and cc is {{{REDACTED-credit-card}}}", response.getFilteredText());

    }

    @Test
    public void endToEnd4() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "profile.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties applicationProperties = new Properties();
        applicationProperties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        applicationProperties.setProperty("store.enabled", "false");
        applicationProperties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        AnonymizationCacheService anonymizationCacheService = new LocalAnonymizationCacheService();
        LocalFilterProfileService filterProfileService = new LocalFilterProfileService(applicationProperties);
        List<FilterProfileService> filterProfileServices = Arrays.asList(filterProfileService);

        PhileasFilterService service = new PhileasFilterService(applicationProperties, filterProfileServices, anonymizationCacheService, "http://localhost:18080/");
        final FilterResponse response = service.filter("default", "context", "test@something.com");

        LOGGER.info(response.getFilteredText());

        Assert.assertEquals("{{{REDACTED-email-address}}}", response.getFilteredText());

    }

    @Test
    public void endToEnd5() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "profile.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties applicationProperties = new Properties();
        applicationProperties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        applicationProperties.setProperty("store.enabled", "false");
        applicationProperties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        AnonymizationCacheService anonymizationCacheService = new LocalAnonymizationCacheService();
        LocalFilterProfileService filterProfileService = new LocalFilterProfileService(applicationProperties);
        List<FilterProfileService> filterProfileServices = Arrays.asList(filterProfileService);

        PhileasFilterService service = new PhileasFilterService(applicationProperties, filterProfileServices, anonymizationCacheService, "http://localhost:18080/");
        final FilterResponse response = service.filter("default", "context", "90210");

        LOGGER.info(response.getFilteredText());

        Assert.assertEquals("{{{REDACTED-zip-code}}}", response.getFilteredText());

    }

    @Test
    public void endToEndMultipleFilterProfiles() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "profile.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        final File file2 = Paths.get(temp.toFile().getAbsolutePath(), "profile.json").toFile();
        LOGGER.info("Writing profile to {}", file2.getAbsolutePath());
        FileUtils.writeStringToFile(file2, gson.toJson(getFilterProfileJustCreditCard("justcreditcard")), Charset.defaultCharset());

        Properties applicationProperties = new Properties();
        applicationProperties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        applicationProperties.setProperty("store.enabled", "false");
        applicationProperties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        AnonymizationCacheService anonymizationCacheService = new LocalAnonymizationCacheService();
        LocalFilterProfileService filterProfileService = new LocalFilterProfileService(applicationProperties);
        List<FilterProfileService> filterProfileServices = Arrays.asList(filterProfileService);

        PhileasFilterService service = new PhileasFilterService(applicationProperties, filterProfileServices, anonymizationCacheService, "http://localhost:18080/");
        final FilterResponse response = service.filter("justcreditcard", "context", "My email is test@something.com");

        LOGGER.info(response.getFilteredText());

        Assert.assertEquals("My email is test@something.com", response.getFilteredText());

    }

    @Test
    public void endToEndJustCreditCard() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final File file2 = Paths.get(temp.toFile().getAbsolutePath(), "profile.json").toFile();
        LOGGER.info("Writing profile to {}", file2.getAbsolutePath());
        FileUtils.writeStringToFile(file2, gson.toJson(getFilterProfileJustCreditCard("justcreditcard")), Charset.defaultCharset());

        Properties applicationProperties = new Properties();
        applicationProperties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        applicationProperties.setProperty("store.enabled", "false");
        applicationProperties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        AnonymizationCacheService anonymizationCacheService = new LocalAnonymizationCacheService();
        LocalFilterProfileService filterProfileService = new LocalFilterProfileService(applicationProperties);
        List<FilterProfileService> filterProfileServices = Arrays.asList(filterProfileService);

        PhileasFilterService service = new PhileasFilterService(applicationProperties, filterProfileServices, anonymizationCacheService, "http://localhost:18080/");
        final FilterResponse response = service.filter("justcreditcard", "context", "My cc is 4121742025464465");

        LOGGER.info(response.getFilteredText());

        Assert.assertEquals("My cc is {{{REDACTED-credit-card}}}", response.getFilteredText());

    }

    @Test
    public void endToEndJustCreditCardWithIgnoredTerms() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final File file2 = Paths.get(temp.toFile().getAbsolutePath(), "profile.json").toFile();
        LOGGER.info("Writing profile to {}", file2.getAbsolutePath());
        FileUtils.writeStringToFile(file2, gson.toJson(getFilterProfileJustCreditCard("justcreditcard")), Charset.defaultCharset());

        Properties applicationProperties = new Properties();
        applicationProperties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        applicationProperties.setProperty("store.enabled", "false");
        applicationProperties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        AnonymizationCacheService anonymizationCacheService = new LocalAnonymizationCacheService();
        LocalFilterProfileService filterProfileService = new LocalFilterProfileService(applicationProperties);
        List<FilterProfileService> filterProfileServices = Arrays.asList(filterProfileService);

        PhileasFilterService service = new PhileasFilterService(applicationProperties, filterProfileServices, anonymizationCacheService, "http://localhost:18080/");
        final FilterResponse response = service.filter("justcreditcard", "context", "My cc is 4121742025464400");

        LOGGER.info(response.getFilteredText());

        Assert.assertEquals("My cc is 4121742025464400", response.getFilteredText());

    }

    @Test
    public void endToEndWithFilterSpecificIgnoredTerms() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "profile.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfileZipCodeWithIgnored("default")), Charset.defaultCharset());

        Properties applicationProperties = new Properties();
        applicationProperties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        applicationProperties.setProperty("store.enabled", "false");
        applicationProperties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        AnonymizationCacheService anonymizationCacheService = new LocalAnonymizationCacheService();
        LocalFilterProfileService filterProfileService = new LocalFilterProfileService(applicationProperties);
        List<FilterProfileService> filterProfileServices = Arrays.asList(filterProfileService);

        PhileasFilterService service = new PhileasFilterService(applicationProperties, filterProfileServices, anonymizationCacheService, "http://localhost:18080/");
        final FilterResponse response = service.filter("default", "context", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.");

        LOGGER.info(response.getFilteredText());

        Assert.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at 90210.", response.getFilteredText());

    }

    @Test(expected = InvalidFilterProfileException.class)
    public void endToEndNonexistentFilterProfile() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "profile.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties applicationProperties = new Properties();
        applicationProperties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        applicationProperties.setProperty("store.enabled", "false");
        applicationProperties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        AnonymizationCacheService anonymizationCacheService = new LocalAnonymizationCacheService();
        LocalFilterProfileService filterProfileService = new LocalFilterProfileService(applicationProperties);
        List<FilterProfileService> filterProfileServices = Arrays.asList(filterProfileService);

        PhileasFilterService service = new PhileasFilterService(applicationProperties, filterProfileServices, anonymizationCacheService, "http://localhost:18080/");
        final FilterResponse response = service.filter("custom1", "context", "My email is test@something.com");

        LOGGER.info(response.getFilteredText());

        Assert.assertEquals("My email is {{{REDACTED-email-address}}}", response.getFilteredText());

    }

    private FilterProfile getFilterProfileZipCodeWithIgnored(String filterProfileName) throws IOException {

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

    private FilterProfile getFilterProfile(String filterProfileName) throws IOException {

        AgeFilterStrategy ageFilterStrategy = new AgeFilterStrategy();

        Age age = new Age();
        age.setAgeFilterStrategies(Arrays.asList(ageFilterStrategy));

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(Arrays.asList(creditCardFilterStrategy));

        DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();

        Date date = new Date();
        date.setDateFilterStrategies(Arrays.asList(dateFilterStrategy));

        EmailAddressFilterStrategy emailAddressFilterStrategy = new EmailAddressFilterStrategy();

        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(Arrays.asList(emailAddressFilterStrategy));

        IdentifierFilterStrategy identifierFilterStrategy = new IdentifierFilterStrategy();

        Identifier identifier = new Identifier();
        identifier.setIdentifierFilterStrategies(Arrays.asList(identifierFilterStrategy));

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

        NerFilterStrategy nerFilterStrategy = new NerFilterStrategy();

        Ner ner = new Ner();
        ner.setNerStrategies(Arrays.asList(nerFilterStrategy));

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
        identifiers.setIdentifiers(Arrays.asList(identifier));
        identifiers.setIpAddress(ipAddress);
        identifiers.setPhoneNumber(phoneNumber);
        identifiers.setSsn(ssn);
        identifiers.setStateAbbreviation(stateAbbreviation);
        identifiers.setUrl(url);
        identifiers.setVin(vin);
        identifiers.setZipCode(zipCode);
        //identifiers.setNer(ner);

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

    private FilterProfile getFilterProfileJustCreditCard(String filterProfileName) throws IOException {

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

}