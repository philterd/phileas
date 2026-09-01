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

import ai.philterd.phileas.PhileasConfiguration;
import com.google.gson.Gson;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.City;
import ai.philterd.phileas.policy.filters.County;
import ai.philterd.phileas.policy.filters.FirstName;
import ai.philterd.phileas.policy.filters.Hospital;
import ai.philterd.phileas.policy.filters.State;
import ai.philterd.phileas.policy.filters.Surname;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.dynamic.CityFilterStrategy;
import ai.philterd.phileas.services.strategies.dynamic.CountyFilterStrategy;
import ai.philterd.phileas.services.strategies.dynamic.FirstNameFilterStrategy;
import ai.philterd.phileas.services.strategies.dynamic.HospitalFilterStrategy;
import ai.philterd.phileas.services.strategies.dynamic.StateFilterStrategy;
import ai.philterd.phileas.services.strategies.dynamic.SurnameFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.Properties;

/**
 * The dictionary-backed filters take their terms from the policy instead of the word list
 * bundled in the jar, so a case can be self-contained.
 */
public class DictionaryTermsFromPolicyTest {

    private final VectorService vectorService = Mockito.mock(VectorService.class);

    // Each row is a filter type, a term supplied by the policy, and a term in the bundled word
    // list that the policy does not supply.
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "surname     | Quorlan       | Smith",
            "first-name  | Thrandia      | Aaban",
            "city        | Zzyzxville    | Abbeville",
            "county      | Vexbridge     | Apache",
            "state       | Morvia        | Alabama",
            "hospital    | Kelbourne     | Mayo Clinic",
    })
    public void policyTermsReplaceTheBundledList(final String type, final String supplied, final String bundled) throws Exception {

        final Policy policy = policyFor(type, List.of(supplied), false);
        final String input = supplied + " and " + bundled + " are here";

        final String filtered = filter(policy, input);

        Assertions.assertFalse(filtered.contains(supplied), "the supplied term should be redacted: " + filtered);
        Assertions.assertTrue(filtered.contains(bundled), "the bundled term should be left alone: " + filtered);

    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "surname     | Smith",
            "first-name  | Aaban",
            "city        | Abbeville",
            "county      | Apache",
            "state       | Alabama",
            "hospital    | Mayo Clinic",
    })
    public void omittingTermsKeepsTheBundledList(final String type, final String bundled) throws Exception {

        final Policy policy = policyFor(type, null, false);

        final String filtered = filter(policy, bundled + " is here");

        Assertions.assertFalse(filtered.contains(bundled), "the bundled term should be redacted: " + filtered);

    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "surname     | Quorlan       | Smith",
            "first-name  | Thrandia      | Aaban",
            "city        | Zzyzxville    | Abbeville",
            "county      | Vexbridge     | Apache",
            "state       | Morvia        | Alabama",
            "hospital    | Kelbourne     | Mayo Clinic",
    })
    public void policyTermsReplaceTheBundledListWhenFuzzy(final String type, final String supplied, final String bundled) throws Exception {

        final Policy policy = policyFor(type, List.of(supplied), true);
        final String input = supplied + " and " + bundled + " are here";

        final String filtered = filter(policy, input);

        Assertions.assertFalse(filtered.contains(supplied), "the supplied term should be redacted: " + filtered);
        Assertions.assertTrue(filtered.contains(bundled), "the bundled term should be left alone: " + filtered);

    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "surname     | Smith",
            "city        | Abbeville",
    })
    public void anEmptyTermsListKeepsTheBundledList(final String type, final String bundled) throws Exception {

        final Policy policy = policyFor(type, List.of(), false);

        final String filtered = filter(policy, bundled + " is here");

        Assertions.assertFalse(filtered.contains(bundled), "the bundled term should be redacted: " + filtered);

    }

    @Test
    public void twoPoliciesDifferingOnlyInTermsDoNotShareCachedFilters() throws Exception {

        // The filter cache is keyed on a hash of the whole policy, so the terms have to be part of
        // that hash or the second policy would reuse the first policy's dictionary.
        final Policy first = policyFor("surname", List.of("Quorlan"), false);
        final Policy second = policyFor("surname", List.of("Thrandia"), false);

        Assertions.assertNotEquals(first.getCacheKey(), second.getCacheKey());

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), vectorService, null);

        final String fromFirst = service.filter(first, "context", "Quorlan and Thrandia").getFilteredText();
        final String fromSecond = service.filter(second, "context", "Quorlan and Thrandia").getFilteredText();

        Assertions.assertFalse(fromFirst.contains("Quorlan"), fromFirst);
        Assertions.assertTrue(fromFirst.contains("Thrandia"), fromFirst);

        Assertions.assertTrue(fromSecond.contains("Quorlan"), fromSecond);
        Assertions.assertFalse(fromSecond.contains("Thrandia"), fromSecond);

    }

    @Test
    public void termsAreReadFromPolicyJson() throws Exception {

        final String json = """
                {
                  "identifiers": {
                    "surname": {
                      "terms": ["Quorlan", "Thrandia"],
                      "surnameFilterStrategies": [ { "strategy": "REDACT", "redactionFormat": "{{{REDACTED-%t}}}" } ]
                    }
                  }
                }
                """;

        final Policy policy = new Gson().fromJson(json, Policy.class);

        Assertions.assertEquals(List.of("Quorlan", "Thrandia"), policy.getIdentifiers().getSurname().getTerms());

        final String filtered = filter(policy, "Quorlan and Thrandia and Smith are here");

        Assertions.assertFalse(filtered.contains("Quorlan"), filtered);
        Assertions.assertFalse(filtered.contains("Thrandia"), filtered);
        Assertions.assertTrue(filtered.contains("Smith"), filtered);

    }

    @Test
    public void aPolicyWithNoTermsKeyStillUsesTheBundledList() throws Exception {

        final String json = """
                {
                  "identifiers": {
                    "surname": {
                      "surnameFilterStrategies": [ { "strategy": "REDACT", "redactionFormat": "{{{REDACTED-%t}}}" } ]
                    }
                  }
                }
                """;

        final Policy policy = new Gson().fromJson(json, Policy.class);

        Assertions.assertNull(policy.getIdentifiers().getSurname().getTerms());
        Assertions.assertFalse(filter(policy, "Smith is here").contains("Smith"));

    }

    @Test
    public void aMultiWordTermIsMatched() throws Exception {

        final Policy policy = policyFor("hospital", List.of("Kelbourne Regional Medical Center"), false);

        final String filtered = filter(policy, "seen at Kelbourne Regional Medical Center today");

        Assertions.assertFalse(filtered.contains("Kelbourne Regional Medical Center"), filtered);
        Assertions.assertTrue(filtered.contains("seen at"), filtered);

    }

    @Test
    public void aSuppliedTermMatchesRegardlessOfCase() throws Exception {

        final Policy policy = policyFor("surname", List.of("Quorlan"), false);

        Assertions.assertFalse(filter(policy, "QUORLAN is here").contains("QUORLAN"));
        Assertions.assertFalse(filter(policy, "quorlan is here").contains("quorlan"));

    }

    @Test
    public void termsCanBeSetFromPhiSQL() throws Exception {

        // The key has to be quoted because TERMS is a PhiSQL keyword, and an array uses brackets.
        final Policy policy = Policy.fromPhiSQL(
                "REDACT SURNAME WITH REDACT OPTIONS ('terms' = ['Quorlan','Thrandia']);");

        Assertions.assertEquals(List.of("Quorlan", "Thrandia"), policy.getIdentifiers().getSurname().getTerms());

        final String filtered = filter(policy, "Quorlan and Thrandia and Smith are here");

        Assertions.assertFalse(filtered.contains("Quorlan"), filtered);
        Assertions.assertFalse(filtered.contains("Thrandia"), filtered);
        Assertions.assertTrue(filtered.contains("Smith"), filtered);

    }

    private String filter(final Policy policy, final String input) throws Exception {

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), vectorService, null);

        final TextFilterResult result = service.filter(policy, "context", input);

        return result.getFilteredText();

    }

    private Policy policyFor(final String type, final List<String> terms, final boolean fuzzy) {

        final Identifiers identifiers = new Identifiers();

        switch (type.trim()) {
            case "surname" -> {
                final Surname surname = new Surname();
                surname.setSurnameFilterStrategies(List.of(new SurnameFilterStrategy()));
                surname.setTerms(terms);
                surname.setFuzzy(fuzzy);
                identifiers.setSurname(surname);
            }
            case "first-name" -> {
                final FirstName firstName = new FirstName();
                firstName.setFirstNameFilterStrategies(List.of(new FirstNameFilterStrategy()));
                firstName.setTerms(terms);
                firstName.setFuzzy(fuzzy);
                identifiers.setFirstName(firstName);
            }
            case "city" -> {
                final City city = new City();
                city.setCityFilterStrategies(List.of(new CityFilterStrategy()));
                city.setTerms(terms);
                city.setFuzzy(fuzzy);
                identifiers.setCity(city);
            }
            case "county" -> {
                final County county = new County();
                county.setCountyFilterStrategies(List.of(new CountyFilterStrategy()));
                county.setTerms(terms);
                county.setFuzzy(fuzzy);
                identifiers.setCounty(county);
            }
            case "state" -> {
                final State state = new State();
                state.setStateFilterStrategies(List.of(new StateFilterStrategy()));
                state.setTerms(terms);
                state.setFuzzy(fuzzy);
                identifiers.setState(state);
            }
            case "hospital" -> {
                final Hospital hospital = new Hospital();
                hospital.setHospitalFilterStrategies(List.of(new HospitalFilterStrategy()));
                hospital.setTerms(terms);
                hospital.setFuzzy(fuzzy);
                identifiers.setHospital(hospital);
            }
            default -> throw new IllegalArgumentException(type);
        }

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return policy;

    }

}
