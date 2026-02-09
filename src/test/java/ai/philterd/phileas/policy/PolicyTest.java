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
import ai.philterd.phileas.policy.filters.PhEye;
import ai.philterd.phileas.policy.filters.PhoneNumber;
import ai.philterd.phileas.policy.filters.PhoneNumberExtension;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.policy.filters.State;
import ai.philterd.phileas.policy.filters.StateAbbreviation;
import ai.philterd.phileas.policy.filters.Surname;
import ai.philterd.phileas.policy.filters.Url;
import ai.philterd.phileas.policy.filters.Vin;
import ai.philterd.phileas.policy.filters.ZipCode;
import ai.philterd.phileas.services.strategies.ai.PhEyeFilterStrategy;
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
import ai.philterd.phileas.services.strategies.rules.PhoneNumberExtensionFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.PhoneNumberFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.StateAbbreviationFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.UrlFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.VinFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.ZipCodeFilterStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PolicyTest {

    private final Gson gson = new Gson();

//    @Test
//    public void compare() throws IOException {
//
//        final Policy policy1 = getPolicy();
//        final String policyJson1 = gson.toJson(policy1);
//
//        final Policy policy2 = getPolicy();
//        final String policyJson2 = gson.toJson(policy2);
//
//        Assertions.assertEquals(policyJson1, policyJson2);
//
//    }

    @Test
    public void serialize() throws IOException {

        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        String json = gson.toJson(getPolicy());

        System.out.println(json);

        Assertions.assertNotNull(json);

    }

    @Test
    public void deserialize1() {

        final String json = """
                {
                  "name": "default",
                  "ignored": [
                    {
                      "name": "ignored-terms",
                      "terms": [
                        "term1",
                        "term2",
                        "Jeff Smith"
                      ]
                    }
                  ],
                  "identifiers": {
                    "dictionaries": [
                      {
                        "type": "mylist",
                        "terms": [
                          "123",
                          "456",
                          "jeff",
                          "john"
                        ],
                        "sensitivity": "auto",
                        "customFilterStrategies": [
                          {
                            "strategy": "REDACT",
                            "redactionFormat": "{{{REDACTED-%t}}}",
                            "replacementScope": "DOCUMENT"
                          }
                        ]
                      }
                    ],
                    "ner": {
                      "nerFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "age": {
                      "ageFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "creditCard": {
                      "creditCardFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "date": {
                      "dateFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "emailAddress": {
                      "emailAddressFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "identifier": {
                      "identifierFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "ipAddress": {
                      "ipAddressFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "phoneNumber": {
                      "phoneNumberFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "ssn": {
                      "ssnFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "url": {
                      "urlFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "vin": {
                      "vinFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "zipCode": {
                      "zipCodeFilterStrategy": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    }
                  }
                }
                """;

        Gson gson = new Gson();
        Policy policy = gson.fromJson(json, Policy.class);

        Assertions.assertTrue(CollectionUtils.isNotEmpty(policy.getIdentifiers().getCustomDictionaries()));
        Assertions.assertTrue(CollectionUtils.isNotEmpty(policy.getIgnored()));
        Assertions.assertTrue(policy.getIdentifiers().hasFilter(FilterType.CUSTOM_DICTIONARY));

    }

    @Test
    public void deserialize2() {

        final String json = """
                {
                  "name": "default",
                  "identifiers": {
                    "ner": {
                      "nerFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "age": {
                      "ageFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "creditCard": {
                      "creditCardFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "date": {
                      "dateFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "emailAddress": {
                      "emailAddressFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "identifier": {
                      "identifierFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "ipAddress": {
                      "ipAddressFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "phoneNumber": {
                      "phoneNumberFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "ssn": {
                      "ssnFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "url": {
                      "urlFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "vin": {
                      "vinFilterStrategies": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    },
                    "zipCode": {
                      "zipCodeFilterStrategy": [
                        {
                          "strategy": "REDACT",
                          "redactionFormat": "{{{REDACTED-%t}}}"
                        }
                      ]
                    }
                  }
                }
                """;

        Gson gson = new Gson();
        Policy policy = gson.fromJson(json, Policy.class);

        Assertions.assertFalse(CollectionUtils.isNotEmpty(policy.getIdentifiers().getCustomDictionaries()));
        Assertions.assertFalse(CollectionUtils.isNotEmpty(policy.getIgnored()));
        Assertions.assertFalse(policy.getIdentifiers().hasFilter(FilterType.CUSTOM_DICTIONARY));

    }

    @Test
    public void deserializeEmpty() {

        final String json = """
                {
                  "identifiers": {
                  }
                }""";

        Gson gson = new Gson();
        Policy policy = gson.fromJson(json, Policy.class);

        Assertions.assertEquals("unnamed", policy.getName());

    }

    private Policy getPolicy() throws IOException {

        CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setTerms(Arrays.asList("123", "456", "jeff", "john"));
        customDictionary.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));

        Age age = new Age();
        age.setAgeFilterStrategies(List.of(new AgeFilterStrategy()));

        City city = new City();
        city.setCityFilterStrategies(List.of(new CityFilterStrategy()));

        County county = new County();
        county.setCountyFilterStrategies(List.of(new CountyFilterStrategy()));

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(List.of(new CreditCardFilterStrategy()));

        Date date = new Date();
        date.setDateFilterStrategies(List.of(new DateFilterStrategy()));

        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(List.of(new EmailAddressFilterStrategy()));

        FirstName firstName = new FirstName();
        firstName.setFirstNameFilterStrategies(List.of(new FirstNameFilterStrategy()));

        Hospital hospital = new Hospital();
        hospital.setHospitalFilterStrategies(List.of(new HospitalFilterStrategy()));

        Identifier identifier = new Identifier();
        identifier.setIdentifierFilterStrategies(List.of(new IdentifierFilterStrategy()));

        IpAddress ipAddress = new IpAddress();
        ipAddress.setIpAddressFilterStrategies(List.of(new IpAddressFilterStrategy()));

        PhEye phEye = new PhEye();
        phEye.setPhEyeFilterStrategies(List.of(new PhEyeFilterStrategy()));

        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPhoneNumberFilterStrategies(List.of(new PhoneNumberFilterStrategy()));

        PhoneNumberExtension phoneNumberExtension = new PhoneNumberExtension();
        phoneNumberExtension.setPhoneNumberExtensionFilterStrategies(List.of(new PhoneNumberExtensionFilterStrategy()));

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(new SsnFilterStrategy()));

        State state = new State();
        state.setStateFilterStrategies(List.of(new StateFilterStrategy()));

        StateAbbreviation stateAbbreviation = new StateAbbreviation();
        stateAbbreviation.setStateAbbreviationsFilterStrategies(List.of(new StateAbbreviationFilterStrategy()));

        Surname surname = new Surname();
        surname.setSurnameFilterStrategies(List.of(new SurnameFilterStrategy()));

        Url url = new Url();
        url.setUrlFilterStrategies(List.of(new UrlFilterStrategy()));

        Vin vin = new Vin();
        vin.setVinFilterStrategies(List.of(new VinFilterStrategy()));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setStrategy("TRUNCATE");
        zipCodeFilterStrategy.setTruncateDigits(2);
        zipCodeFilterStrategy.setConditions("population < 4500");

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(List.of(zipCodeFilterStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setCustomDictionaries(List.of(customDictionary));
        identifiers.setAge(age);
        identifiers.setCity(city);
        identifiers.setCounty(county);
        identifiers.setCreditCard(creditCard);
        identifiers.setDate(date);
        identifiers.setEmailAddress(emailAddress);
        identifiers.setFirstName(firstName);
        identifiers.setHospital(hospital);
        identifiers.setIdentifiers(List.of(identifier));
        identifiers.setIpAddress(ipAddress);
        identifiers.setPerson(phEye);
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
        policy.setIgnored(List.of(ignored));

        return policy;

    }

}
