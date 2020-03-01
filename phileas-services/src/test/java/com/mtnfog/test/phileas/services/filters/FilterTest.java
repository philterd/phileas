package com.mtnfog.test.phileas.services.filters;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Identifiers;
import com.mtnfog.phileas.model.profile.filters.*;
import com.mtnfog.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.*;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.*;
import com.mtnfog.phileas.services.anonymization.AgeAnonymizationService;
import com.mtnfog.phileas.services.cache.LocalAnonymizationCacheService;
import com.mtnfog.phileas.services.filters.regex.AgeFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FilterTest {

    protected static final Logger LOGGER = LogManager.getLogger(FilterTest.class);

    @Test
    public void window0() throws Exception {

        // This tests span window creation.
        final AgeFilter filter = new AgeFilter(null, new AgeAnonymizationService(new LocalAnonymizationCacheService()), Collections.emptySet(), new Crypto());

        List<Span> spans = filter.filter(getFilterProfile(), "context", "documentid", "this is a first sentence. the patient is 3.5 years old and he's cool. this is a surrounding sentence.");

        showSpans(spans);

        final String[] window = new String[]{"the","patient","is","3.5","years","old","and","he's","cool."};

        Assert.assertEquals(1, spans.size());
        Assert.assertTrue(checkSpan(spans.get(0), 41, 54, FilterType.AGE));
        Assert.assertEquals("{{{REDACTED-age}}}", spans.get(0).getReplacement());
        Assert.assertArrayEquals("Window spans do not equal.", window, spans.get(0).getWindow());

    }

    /**
     * Gets a {@link FilterProfile} where all non-deterministic filters use a MEDIUM {@link SensitivityLevel}.
     * @return A {@link FilterProfile} where all non-deterministic filters use a MEDIUM {@link SensitivityLevel}.
     */
    public FilterProfile getFilterProfile() throws IOException {
        return getFilterProfile(SensitivityLevel.MEDIUM);
    }

    /**
     * Gets a {@link FilterProfile} where all non-deterministic filters use the given {@link SensitivityLevel}.
     * @param sensitivityLevel The {@link SensitivityLevel} for all non-deterministic filters.
     * @return A {@link FilterProfile} where all non-deterministic filters use the given {@link SensitivityLevel}.
     */
    public FilterProfile getFilterProfile(SensitivityLevel sensitivityLevel) throws IOException {

        CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setCustomDictionaryFilterStrategies(Arrays.asList(new CustomDictionaryFilterStrategy()));

        Age age = new Age();
        age.setAgeFilterStrategies(Arrays.asList(new AgeFilterStrategy()));

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(Arrays.asList(new CreditCardFilterStrategy()));

        Date date = new Date();
        date.setDateFilterStrategies(Arrays.asList(new DateFilterStrategy()));

        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(Arrays.asList(new EmailAddressFilterStrategy()));

        Identifier identifier = new Identifier();
        identifier.setIdentifierFilterStrategies(Arrays.asList(new IdentifierFilterStrategy()));

        IpAddress ipAddress = new IpAddress();
        ipAddress.setIpAddressFilterStrategies(Arrays.asList(new IpAddressFilterStrategy()));

        MacAddress macAddress = new MacAddress();
        macAddress.setMacAddressFilterStrategies(Arrays.asList(new MacAddressFilterStrategy()));

        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPhoneNumberFilterStrategies(Arrays.asList(new PhoneNumberFilterStrategy()));

        PhoneNumberExtension phoneNumberExtension = new PhoneNumberExtension();
        phoneNumberExtension.setPhoneNumberExtensionFilterStrategies(Arrays.asList(new PhoneNumberExtensionFilterStrategy()));

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(Arrays.asList(new SsnFilterStrategy()));

        StateAbbreviation stateAbbreviation = new StateAbbreviation();
        stateAbbreviation.setStateAbbreviationsFilterStrategies(Arrays.asList(new StateAbbreviationFilterStrategy()));

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
        identifiers.setCreditCard(creditCard);
        identifiers.setDate(date);
        identifiers.setEmailAddress(emailAddress);
        identifiers.setIdentifiers(Arrays.asList(identifier));
        identifiers.setIpAddress(ipAddress);
        identifiers.setMacAddress(macAddress);
        identifiers.setPhoneNumber(phoneNumber);
        identifiers.setPhoneNumberExtension(phoneNumberExtension);
        identifiers.setSsn(ssn);
        identifiers.setStateAbbreviation(stateAbbreviation);
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

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName("default");
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    public String getIndexDirectory(String indexName) {

        final String baseDir = System.getenv("PHILEAS_INDER_DIR");

        if(!StringUtils.isEmpty(baseDir)) {

            final String indexDirectory = baseDir + "/data/indexes/" + indexName;

            LOGGER.info("Using index directory: " + indexDirectory);

            return indexDirectory;

        } else {

            LOGGER.warn("Environment variable PHILEAS_INDER_DIR is not set for Lucene index test.");

            return "/mtnfog/code/bitbucket/philter/phileas/data/indexes/" + indexName;

        }

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

}
