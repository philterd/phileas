package com.mtnfog.test.phileas.model.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Identifiers;
import com.mtnfog.phileas.model.profile.filters.*;
import com.mtnfog.phileas.model.profile.filters.strategies.ai.NerFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.*;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class FilterProfileTest {

    @Test
    public void serialize() throws IOException {

        Age age = new Age();
        age.setAgeFilterStrategies(Arrays.asList(new AgeFilterStrategy()));

        City city = new City();
        city.setCityFilterStrategies(Arrays.asList(new CityFilterStrategy()));

        County county = new County();
        county.setCountyFilterStrategies(Arrays.asList(new CountyFilterStrategy()));

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(Arrays.asList(new CreditCardFilterStrategy()));

        Date date = new Date();
        date.setDateFilterStrategies(Arrays.asList(new DateFilterStrategy()));

        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(Arrays.asList(new EmailAddressFilterStrategy()));

        FirstName firstName = new FirstName();
        firstName.setFirstNameFilterStrategies(Arrays.asList(new FirstNameFilterStrategy()));

        Hospital hospital = new Hospital();
        hospital.setHospitalFilterStrategies(Arrays.asList(new HospitalFilterStrategy()));

        HospitalAbbreviation hospitalAbbreviation = new HospitalAbbreviation();
        hospitalAbbreviation.setHospitalAbbreviationFilterStrategies(Arrays.asList(new HospitalAbbreviationFilterStrategy()));

        Identifier identifier = new Identifier();
        identifier.setIdentifierFilterStrategies(Arrays.asList(new IdentifierFilterStrategy()));

        IpAddress ipAddress = new IpAddress();
        ipAddress.setIpAddressFilterStrategies(Arrays.asList(new IpAddressFilterStrategy()));

        Ner ner = new Ner();
        ner.setNerStrategies(Arrays.asList(new NerFilterStrategy()));

        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPhoneNumberFilterStrategies(Arrays.asList(new PhoneNumberFilterStrategy()));

        PhoneNumberExtension phoneNumberExtension = new PhoneNumberExtension();
        phoneNumberExtension.setPhoneNumberExtensionFilterStrategies(Arrays.asList(new PhoneNumberExtensionFilterStrategy()));

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(Arrays.asList(new SsnFilterStrategy()));

        State state = new State();
        state.setStateFilterStrategies(Arrays.asList(new StateFilterStrategy()));

        StateAbbreviation stateAbbreviation = new StateAbbreviation();
        stateAbbreviation.setStateAbbreviationsFilterStrategies(Arrays.asList(new StateAbbreviationFilterStrategy()));

        Surname surname = new Surname();
        surname.setSurnameFilterStrategies(Arrays.asList(new SurnameFilterStrategy()));

        Url url = new Url();
        url.setUrlFilterStrategies(Arrays.asList(new UrlFilterStrategy()));

        Vin vin = new Vin();
        vin.setVinFilterStrategies(Arrays.asList(new VinFilterStrategy()));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setStrategy("TRUNCATE");
        zipCodeFilterStrategy.setTruncateDigits(2);
        zipCodeFilterStrategy.setConditions("population < 4500");

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setAge(age);
        identifiers.setCity(city);
        identifiers.setCounty(county);
        identifiers.setCreditCard(creditCard);
        identifiers.setDate(date);
        identifiers.setEmailAddress(emailAddress);
        identifiers.setFirstName(firstName);
        identifiers.setHospital(hospital);
        identifiers.setHospitalAbbreviation(hospitalAbbreviation);
        identifiers.setIdentifiers(Arrays.asList(identifier));
        identifiers.setIpAddress(ipAddress);
        identifiers.setNer(ner);
        identifiers.setPhoneNumber(phoneNumber);
        identifiers.setPhoneNumberExtension(phoneNumberExtension);
        identifiers.setSsn(ssn);
        identifiers.setState(state);
        identifiers.setStateAbbreviation(stateAbbreviation);
        identifiers.setSurname(surname);
        identifiers.setUrl(url);
        identifiers.setVin(vin);
        identifiers.setZipCode(zipCode);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName("default");
        filterProfile.setIdentifiers(identifiers);

        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        String json = gson.toJson(filterProfile);

        System.out.println(json);

    }

    @Test
    public void deserialize() {

        final String json = "{\n" +
                "  \"name\": \"default\",\n" +
                "  \"identifiers\": {\n" +
                "    \"age\": {\n" +
                "      \"ageFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"creditCard\": {\n" +
                "      \"creditCardFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"date\": {\n" +
                "      \"dateFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"emailAddress\": {\n" +
                "      \"emailAddressFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"identifiers\": [{\n" +
                "      \"identifierFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }],\n" +
                "    \"ipAddress\": {\n" +
                "      \"ipAddressFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"phoneNumber\": {\n" +
                "      \"phoneNumberFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"ssn\": {\n" +
                "      \"ssnFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"stateAbbreviation\": {\n" +
                "      \"stateAbbreviationFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"url\": {\n" +
                "      \"urlFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"vin\": {\n" +
                "      \"vinFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"zipCode\": {\n" +
                "      \"zipCodeFilterStrategy\": [\n" +
                "        {\n" +
                "          \"truncateDigits\": 2,\n" +
                "          \"strategy\": \"TRUNCATE\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\",\n" +
                "          \"conditions\": \"population \\u003c 4500\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}\n";

        Gson gson = new Gson();
        FilterProfile filterProfile = gson.fromJson(json, FilterProfile.class);

    }

    @Test
    public void deserializeEmpty() {

        final String json = "{\n" +
                "  \"identifiers\": {\n" +
                "  }\n" +
                "}";

        Gson gson = new Gson();
        FilterProfile filterProfile = gson.fromJson(json, FilterProfile.class);

    }

}
