/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.policy.InvalidPolicyPatternException;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Properties;

/**
 * The NPI, DEA number, MBI, health plan member ID, and device identifier filters through a policy.
 * Values are published examples or invented.
 */
public class HealthcareIdentifiersEndToEndTest {

    private static final String ALL_FILTERS = """
            {
              "identifiers": {
                "npi": { "npiFilterStrategies": [ { "strategy": "REDACT" } ] },
                "deaNumber": { "deaNumberFilterStrategies": [ { "strategy": "REDACT" } ] },
                "mbi": { "mbiFilterStrategies": [ { "strategy": "REDACT" } ] },
                "ssn": { "ssnFilterStrategies": [ { "strategy": "REDACT" } ] },
                "phoneNumber": { "phoneNumberFilterStrategies": [ { "strategy": "REDACT" } ] },
                "date": { "dateFilterStrategies": [ { "strategy": "REDACT" } ] },
                "healthPlanMemberId": {
                  "priority": 10,
                  "cueWords": [ "Medicare ID" ],
                  "healthPlanMemberIdFilterStrategies": [ { "strategy": "REDACT" } ]
                },
                "deviceIdentifier": { "priority": 10, "deviceIdentifierFilterStrategies": [ { "strategy": "REDACT" } ] }
              }
            }
            """;

    private static String filter(final String policyJson, final String text) throws Exception {
        final Policy policy = new Gson().fromJson(policyJson, Policy.class);
        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
        return service.filter(policy, "context", text).getFilteredText();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Provider NPI 1234567893.                 | Provider NPI {{{REDACTED-npi}}}.",
            "DEA AB1234563 on file.                   | DEA {{{REDACTED-dea-number}}} on file.",
            "MBI 1EG4-TE5-MK73 on file.               | MBI {{{REDACTED-mbi}}} on file.",
            "Member ID: XYZ123456789                  | Member ID: {{{REDACTED-health-plan-member-id}}}",
            "UDI (01)00812345000016(21)SN0042 placed. | UDI {{{REDACTED-device-identifier}}} placed.",
            "Pump S/N: PM-77231 replaced.             | Pump S/N: {{{REDACTED-device-identifier}}} replaced."
    })
    public void eachFilterRedacts(final String text, final String expected) throws Exception {
        Assertions.assertEquals(expected, filter(ALL_FILTERS, text));
    }

    // The cue word filters have the higher priority in the policy, and in the phone number cases the cue-matched
    // value is the longer span, so only the precedence rule can keep these.
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Medicare ID: 1EG4-TE5-MK73      | Medicare ID: {{{REDACTED-mbi}}}",
            "Member ID: 123-45-6789          | Member ID: {{{REDACTED-ssn}}}",
            "Member ID: 1234567893           | Member ID: {{{REDACTED-npi}}}",
            "Member ID: 555-555-0123X        | Member ID: {{{REDACTED-phone-number}}}X",
            "Member ID: 2024-01-15           | Member ID: {{{REDACTED-date}}}",
            "S/N: 555-555-0123X              | S/N: {{{REDACTED-phone-number}}}X"
    })
    public void aValidatedIdentifierTakesPriorityOverACueMatchedValue(final String text, final String expected) throws Exception {
        Assertions.assertEquals(expected, filter(ALL_FILTERS, text));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Medicare ID: 1EG4-TE5-MK73      | Medicare ID: {{{REDACTED-mbi}}}",
            "Member ID: 1234567893           | Member ID: {{{REDACTED-npi}}}",
            "Member ID: 123-45-6789          | Member ID: {{{REDACTED-ssn}}}"
    })
    public void theValidatedIdentifierTakesPriorityWithSpanDisambiguation(final String text, final String expected) throws Exception {

        // Disambiguation picks one type for spans covering the same text, which must not undo the precedence.
        final Properties properties = new Properties();
        properties.setProperty("span.disambiguation.enabled", "true");

        final Policy policy = new Gson().fromJson(ALL_FILTERS, Policy.class);
        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(properties),
                new DefaultContextService(), new InMemoryVectorService(), null);

        // Train the vectors on unambiguous member IDs after the same cue words, so disambiguation favors the
        // member ID type for the ambiguous text.
        for (int i = 0; i < 20; i++) {
            service.filter(policy, "context", "Member ID: XYZ1234567" + (i % 10) + " Medicare ID: XYZ7654321" + (i % 10));
        }

        Assertions.assertEquals(expected, service.filter(policy, "context", text).getFilteredText());

    }

    @Test
    public void aCueMatchedValueIsKeptWhereTheOverlappingSpanChangesNoText() throws Exception {

        // The date strategy's condition is not satisfied, so the date is left unchanged and does not hide the member ID.
        final String policy = """
                {
                  "identifiers": {
                    "date": { "dateFilterStrategies": [ { "strategy": "REDACT", "condition": "context == \\"other\\"" } ] },
                    "healthPlanMemberId": { "healthPlanMemberIdFilterStrategies": [ { "strategy": "REDACT" } ] }
                  }
                }
                """;

        Assertions.assertEquals("Member ID: {{{REDACTED-health-plan-member-id}}}", filter(policy, "Member ID: 2024-01-15"));

    }

    @Test
    public void cueWordOptionsAreReadFromThePolicy() throws Exception {

        final String policy = """
                {
                  "identifiers": {
                    "healthPlanMemberId": {
                      "patterns": [ "\\\\b[A-Z]{3}[0-9]{9}\\\\b" ],
                      "cueWords": [ "Plan ID" ],
                      "defaultCueWords": false,
                      "healthPlanMemberIdFilterStrategies": [ { "strategy": "REDACT" } ]
                    }
                  }
                }
                """;

        Assertions.assertEquals("Plan ID: {{{REDACTED-health-plan-member-id}}}", filter(policy, "Plan ID: XYZ123456789"));
        Assertions.assertEquals("Member ID: XYZ123456789", filter(policy, "Member ID: XYZ123456789"));
        Assertions.assertEquals("Plan ID: 12345678", filter(policy, "Plan ID: 12345678"));

    }

    // The member ID and device identifier filters with the given priorities. The device identifier filter also takes
    // "MRN" as a cue word, standing in for another cue word filter.
    private static String cueWordFilters(final int memberIdPriority, final int devicePriority) {
        return """
                {
                  "identifiers": {
                    "healthPlanMemberId": { "priority": %d },
                    "deviceIdentifier": { "priority": %d, "cueWords": [ "MRN" ] }
                  }
                }
                """.formatted(memberIdPriority, devicePriority);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Serial No: SN-998877 Member ID ABC12345 | Serial No: {{{REDACTED-device-identifier}}} Member ID {{{REDACTED-health-plan-member-id}}}",
            "Member ID ABC12345 Serial No: SN-998877 | Member ID {{{REDACTED-health-plan-member-id}}} Serial No: {{{REDACTED-device-identifier}}}",
            "Member ID ABC12345, MRN 123456          | Member ID {{{REDACTED-health-plan-member-id}}}, MRN {{{REDACTED-device-identifier}}}",
            "MRN 123456, Member ID ABC12345          | MRN {{{REDACTED-device-identifier}}}, Member ID {{{REDACTED-health-plan-member-id}}}",
            "Member ID: 778899 and 112233            | Member ID: {{{REDACTED-health-plan-member-id}}} and {{{REDACTED-health-plan-member-id}}}",
            "Member ID: 778899 Device ID: 112233     | Member ID: {{{REDACTED-health-plan-member-id}}} Device ID: {{{REDACTED-device-identifier}}}"
    })
    public void aValueTakesTheTypeOfItsNearestCueWord(final String text, final String expected) throws Exception {

        // Whichever filter has the higher priority, the nearest cue word decides.
        Assertions.assertEquals(expected, filter(cueWordFilters(0, 0), text));
        Assertions.assertEquals(expected, filter(cueWordFilters(5, 0), text));
        Assertions.assertEquals(expected, filter(cueWordFilters(0, 5), text));

    }

    @Test
    public void aValueTakesTheTypeOfItsNearestCueWordWithSpanDisambiguation() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("span.disambiguation.enabled", "true");

        final Policy policy = new Gson().fromJson(cueWordFilters(0, 0), Policy.class);
        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(properties),
                new DefaultContextService(), new InMemoryVectorService(), null);

        // Train the vectors so disambiguation favors the member ID type.
        for (int i = 0; i < 20; i++) {
            service.filter(policy, "context", "Member ID: XYZ1234567" + (i % 10));
        }

        Assertions.assertEquals("Member ID {{{REDACTED-health-plan-member-id}}} Serial No: {{{REDACTED-device-identifier}}}",
                service.filter(policy, "context", "Member ID ABC12345 Serial No: SN-998877").getFilteredText());

    }

    @Test
    public void aCueWordOfAFilterNotInThePolicyDoesNotCompete() throws Exception {

        final String policy = """
                { "identifiers": { "healthPlanMemberId": { } } }
                """;

        Assertions.assertEquals("Member ID {{{REDACTED-health-plan-member-id}}}, MRN {{{REDACTED-health-plan-member-id}}}",
                filter(policy, "Member ID ABC12345, MRN 123456"));

        Assertions.assertEquals("Member ID {{{REDACTED-health-plan-member-id}}} Serial No: {{{REDACTED-health-plan-member-id}}}",
                filter(policy, "Member ID ABC12345 Serial No: SN-998877"));

        Assertions.assertEquals("Serial No: {{{REDACTED-device-identifier}}} Member ID {{{REDACTED-device-identifier}}}",
                filter("{ \"identifiers\": { \"deviceIdentifier\": { } } }", "Serial No: SN-998877 Member ID ABC12345"));

    }

    @Test
    public void aValueAfterANearerCueWordThatChangesNoTextDoesNotTakePriority() throws Exception {

        // The device identifier strategy's condition is not satisfied, so its value changes no text and the
        // member ID is kept.
        final String policy = """
                {
                  "identifiers": {
                    "healthPlanMemberId": { },
                    "deviceIdentifier": {
                      "deviceIdentifierFilterStrategies": [ { "strategy": "REDACT", "condition": "context == \\"other\\"" } ]
                    }
                  }
                }
                """;

        Assertions.assertEquals("Member ID {{{REDACTED-health-plan-member-id}}} Serial No: {{{REDACTED-health-plan-member-id}}}",
                filter(policy, "Member ID ABC12345 Serial No: SN-998877"));

    }

    @Test
    public void aValidatedIdentifierStillTakesPriorityOverTheNearestCueWord() throws Exception {

        Assertions.assertEquals("Member ID {{{REDACTED-health-plan-member-id}}} Serial No: {{{REDACTED-ssn}}}",
                filter(ALL_FILTERS, "Member ID ABC12345 Serial No: 123-45-6789"));

    }

    @Test
    public void defaultCueWordsFalseWithNoCueWordsRefusesThePolicy() {

        final String policy = """
                { "identifiers": { "deviceIdentifier": { "defaultCueWords": false } } }
                """;

        Assertions.assertThrows(IllegalArgumentException.class, () -> filter(policy, "S/N: SN0042"));

    }

    @Test
    public void anInvalidPatternRefusesThePolicy() {

        final String policy = """
                { "identifiers": { "healthPlanMemberId": { "patterns": [ "[A-Z" ] } } }
                """;

        Assertions.assertThrows(InvalidPolicyPatternException.class, () -> filter(policy, "Member ID: XYZ123456789"));

    }

}
