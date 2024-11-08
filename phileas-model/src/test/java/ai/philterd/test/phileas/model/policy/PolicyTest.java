/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.test.phileas.model.policy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.Identifiers;
import ai.philterd.phileas.model.policy.Ignored;
import ai.philterd.phileas.model.policy.filters.*;
import ai.philterd.phileas.model.policy.filters.strategies.ai.PhEyeFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.custom.CustomDictionaryFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.*;
import ai.philterd.phileas.model.policy.filters.strategies.rules.*;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

public class PolicyTest {

    @Test
    public void serialize() throws IOException {

        CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setTerms(Arrays.asList("123", "456", "jeff", "john"));
        customDictionary.setSensitivity(SensitivityLevel.AUTO.getName());
        customDictionary.setCustomDictionaryFilterStrategies(Arrays.asList(new CustomDictionaryFilterStrategy()));

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

        PhEyeFilter phEyeFilter = new PhEyeFilter();
        phEyeFilter.setPhEyeFilterStrategies(Arrays.asList(new PhEyeFilterStrategy()));

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
        identifiers.setCustomDictionaries(Arrays.asList(customDictionary));
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
        identifiers.setPerson(phEyeFilter);
        identifiers.setPhoneNumber(phoneNumber);
        identifiers.setPhoneNumberExtension(phoneNumberExtension);
        identifiers.setSsn(ssn);
        identifiers.setState(state);
        identifiers.setStateAbbreviation(stateAbbreviation);
        identifiers.setSurname(surname);
        identifiers.setUrl(url);
        identifiers.setVin(vin);
        identifiers.setZipCode(zipCode);

        Ignored ignored = new Ignored();
        ignored.setName("ignored-terms");
        ignored.setTerms(Arrays.asList("term1", "term2"));

        Policy policy = new Policy();
        policy.setName("default");
        policy.setIdentifiers(identifiers);
        policy.setIgnored(Arrays.asList(ignored));

        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        String json = gson.toJson(policy);

        System.out.println(json);

        Assertions.assertNotNull(json);

    }

    @Test
    public void deserialize1() {

        final String json = "{\n" +
                "  \"name\": \"default\",\n" +
                "  \"ignored\": [\n" +
                "    {\n" +
                "      \"name\": \"ignored-terms\",\n" +
                "      \"terms\": [\n" +
                "        \"term1\",\n" +
                "        \"term2\",\n" +
                "        \"Jeff Smith\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"identifiers\": {\n" +
                "    \"dictionaries\": [\n" +
                "      {\n" +
                "        \"type\": \"mylist\",\n" +
                "        \"terms\": [\n" +
                "          \"123\",\n" +
                "          \"456\",\n" +
                "          \"jeff\",\n" +
                "          \"john\"\n" +
                "        ],\n" +
                "        \"sensitivity\": \"auto\",\n" +
                "        \"customFilterStrategies\": [\n" +
                "          {\n" +
                "            \"strategy\": \"REDACT\",\n" +
                "            \"redactionFormat\": \"{{{REDACTED-%t}}}\",\n" +
                "            \"replacementScope\": \"DOCUMENT\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ],\n" +
                "    \"ner\": {\n" +
                "      \"nerFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"age\": {\n" +
                "      \"ageFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"creditCard\": {\n" +
                "      \"creditCardFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"date\": {\n" +
                "      \"dateFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"emailAddress\": {\n" +
                "      \"emailAddressFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"identifier\": {\n" +
                "      \"identifierFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"ipAddress\": {\n" +
                "      \"ipAddressFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"phoneNumber\": {\n" +
                "      \"phoneNumberFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"ssn\": {\n" +
                "      \"ssnFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"url\": {\n" +
                "      \"urlFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"vin\": {\n" +
                "      \"vinFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"zipCode\": {\n" +
                "      \"zipCodeFilterStrategy\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}\n";

        Gson gson = new Gson();
        Policy policy = gson.fromJson(json, Policy.class);

        Assertions.assertTrue(CollectionUtils.isNotEmpty(policy.getIdentifiers().getCustomDictionaries()));
        Assertions.assertTrue(CollectionUtils.isNotEmpty(policy.getIgnored()));
        Assertions.assertTrue(policy.getIdentifiers().hasFilter(FilterType.CUSTOM_DICTIONARY));

    }

    @Test
    public void deserialize2() {

        final String json = "{\n" +
                "  \"name\": \"default\",\n" +
                "  \"identifiers\": {\n" +
                "    \"ner\": {\n" +
                "      \"nerFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"age\": {\n" +
                "      \"ageFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"creditCard\": {\n" +
                "      \"creditCardFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"date\": {\n" +
                "      \"dateFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"emailAddress\": {\n" +
                "      \"emailAddressFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"identifier\": {\n" +
                "      \"identifierFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"ipAddress\": {\n" +
                "      \"ipAddressFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"phoneNumber\": {\n" +
                "      \"phoneNumberFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"ssn\": {\n" +
                "      \"ssnFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"url\": {\n" +
                "      \"urlFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"vin\": {\n" +
                "      \"vinFilterStrategies\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"zipCode\": {\n" +
                "      \"zipCodeFilterStrategy\": [\n" +
                "        {\n" +
                "          \"strategy\": \"REDACT\",\n" +
                "          \"redactionFormat\": \"{{{REDACTED-%t}}}\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}\n";

        Gson gson = new Gson();
        Policy policy = gson.fromJson(json, Policy.class);

        Assertions.assertFalse(CollectionUtils.isNotEmpty(policy.getIdentifiers().getCustomDictionaries()));
        Assertions.assertFalse(CollectionUtils.isNotEmpty(policy.getIgnored()));
        Assertions.assertFalse(policy.getIdentifiers().hasFilter(FilterType.CUSTOM_DICTIONARY));

    }

    @Test
    public void deserializeEmpty() {

        final String json = "{\n" +
                "  \"identifiers\": {\n" +
                "  }\n" +
                "}";

        Gson gson = new Gson();
        Policy policy = gson.fromJson(json, Policy.class);

    }

}
