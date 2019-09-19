package com.mtnfog.test.phileas.model.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Identifiers;
import com.mtnfog.phileas.model.profile.filters.*;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class FilterProfileTest {

    @Test
    public void serialize() throws IOException {

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

        StateAbbreviationsFilterStrategy stateAbbreviationsFilterStrategy = new StateAbbreviationsFilterStrategy();

        StateAbbreviation stateAbbreviation = new StateAbbreviation();
        stateAbbreviation.setStateAbbreviationsFilterStrategies(Arrays.asList(stateAbbreviationsFilterStrategy));

        UrlFilterStrategy urlFilterStrategy = new UrlFilterStrategy();

        Url url = new Url();
        url.setUrlFilterStrategies(Arrays.asList(urlFilterStrategy));

        VinFilterStrategy vinFilterStrategy = new VinFilterStrategy();

        Vin vin = new Vin();
        vin.setVinFilterStrategies(Arrays.asList(vinFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setStrategy("TRUNCATE");
        zipCodeFilterStrategy.setTruncateDigits(2);
        zipCodeFilterStrategy.setConditions("population < 4500");

        List<ZipCodeFilterStrategy> zipCodeFilterStrategies = new LinkedList<ZipCodeFilterStrategy>();
        zipCodeFilterStrategies.add(zipCodeFilterStrategy);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(zipCodeFilterStrategies);

        Identifiers identifiers = new Identifiers();
        identifiers.setAge(age);
        identifiers.setCreditCard(creditCard);
        identifiers.setDate(date);
        identifiers.setEmailAddress(emailAddress);
        identifiers.setIdentifier(identifier);
        identifiers.setIpAddress(ipAddress);
        identifiers.setPhoneNumber(phoneNumber);
        identifiers.setSsn(ssn);
        identifiers.setStateAbbreviation(stateAbbreviation);
        identifiers.setUrl(url);
        identifiers.setVin(vin);
        identifiers.setZipCode(zipCode);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName("default");
        filterProfile.setIdentifiers(identifiers);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
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
                "    \"identifier\": {\n" +
                "      \"identifierFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "          \"sensitivityLevel\": \"high\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
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
