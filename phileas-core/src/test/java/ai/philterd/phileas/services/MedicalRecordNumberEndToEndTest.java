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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Properties;

/**
 * The medical record number filter through a policy. All values are invented.
 */
public class MedicalRecordNumberEndToEndTest {

    private static final String CRYPTO = "\"crypto\": {\"key\": \"9EE7A356FDFE43F069500B0086758346E66D8583E0CE1CFCA04E50F67ECCE5D1\", "
            + "\"iv\": \"B674D3B8F1C025AEFF8F6D5FA1AEAD3A\"}";
    private static final String FPE = "\"fpe\": {\"key\": \"EF4359D8D580AA4F7F036D6F04FC6A94\", \"tweak\": \"D8E7920AFA330A73\"}";

    // The medical record number filter has the higher priority, so only the precedence rule can keep the others.
    private static final String WITH_VALIDATED_FILTERS = """
            {
              "identifiers": {
                "npi": { },
                "mbi": { },
                "ssn": { },
                "phoneNumber": { },
                "date": { },
                "healthPlanMemberId": { },
                "medicalRecordNumber": { "priority": 10 }
              }
            }
            """;

    private static String filter(final String policyJson, final String text) throws Exception {
        final Policy policy = new Gson().fromJson(policyJson, Policy.class);
        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
        return service.filter(policy, "context", text).getFilteredText();
    }

    private static String strategy(final String strategy, final String extra) {
        return "{" + CRYPTO + ", " + FPE + ", \"identifiers\": {\"medicalRecordNumber\": {"
                + "\"medicalRecordNumberFilterStrategies\": [{\"strategy\": \"" + strategy + "\"" + extra + "}]}}}";
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "MRN: 00482913 on file.                  | MRN: {{{REDACTED-medical-record-number}}} on file.",
            "The MRN for this visit is 00482913.     | The MRN for this visit is {{{REDACTED-medical-record-number}}}.",
            "Medical Record No: A1234567             | Medical Record No: {{{REDACTED-medical-record-number}}}",
            "MR#7731-0042                            | MR#{{{REDACTED-medical-record-number}}}"
    })
    public void aNumberAfterACueWordIsRedacted(final String text, final String expected) throws Exception {
        Assertions.assertEquals(expected, filter("{ \"identifiers\": { \"medicalRecordNumber\": { } } }", text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Reference 00482913 on file.", "Patient ID: 00482913"})
    public void aNumberWithNoCueWordIsLeftAlone(final String text) throws Exception {
        Assertions.assertEquals(text, filter("{ \"identifiers\": { \"medicalRecordNumber\": { } } }", text));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "MRN: 123-45-6789          | MRN: {{{REDACTED-ssn}}}",
            "MRN: 1234567893           | MRN: {{{REDACTED-npi}}}",
            "MRN: 1EG4TE5MK73          | MRN: {{{REDACTED-mbi}}}",
            "MRN: 555-555-0123         | MRN: {{{REDACTED-phone-number}}}",
            "MRN: 2024-01-15           | MRN: {{{REDACTED-date}}}"
    })
    public void aValidatedIdentifierTakesPriorityOverAMedicalRecordNumber(final String text, final String expected) throws Exception {

        // Each value is a medical record number on its own, so the other filter wins by precedence, not by length.
        Assertions.assertEquals("MRN: {{{REDACTED-medical-record-number}}}",
                filter("{ \"identifiers\": { \"medicalRecordNumber\": { } } }", text));

        Assertions.assertEquals(expected, filter(WITH_VALIDATED_FILTERS, text));

    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Member ID ABC12345 MRN: 00482913 | Member ID {{{REDACTED-health-plan-member-id}}} MRN: {{{REDACTED-medical-record-number}}}",
            "MRN: 00482913 Member ID ABC12345 | MRN: {{{REDACTED-medical-record-number}}} Member ID {{{REDACTED-health-plan-member-id}}}"
    })
    public void aValueTakesTheTypeOfItsNearestCueWord(final String text, final String expected) throws Exception {
        Assertions.assertEquals(expected, filter(WITH_VALIDATED_FILTERS, text));
    }

    @Test
    public void everyBaseStrategyWorks() throws Exception {

        final String text = "MRN: 00482913";

        Assertions.assertEquals("MRN: 2913", filter(strategy("LAST_4", ""), text));
        Assertions.assertEquals("MRN: ********", filter(strategy("MASK", ""), text));
        Assertions.assertEquals("MRN: X", filter(strategy("STATIC_REPLACE", ", \"staticReplacement\": \"X\""), text));
        Assertions.assertEquals("MRN: 0048****", filter(strategy("TRUNCATE", ""), text));
        Assertions.assertEquals("MRN: {{{REDACTED-medical-record-number}}}", filter(strategy("REDACT", ""), text));

        for (final String strategy : new String[]{"RANDOM_REPLACE", "CRYPTO_REPLACE", "FPE_ENCRYPT_REPLACE", "HASH_SHA256_REPLACE"}) {
            final String redacted = filter(strategy(strategy, ""), text);
            Assertions.assertTrue(redacted.startsWith("MRN: "), strategy + ": " + redacted);
            Assertions.assertFalse(redacted.contains("00482913"), strategy + ": " + redacted);
        }

        final String fpe = filter(strategy("FPE_ENCRYPT_REPLACE", ""), text);
        Assertions.assertTrue(fpe.matches("MRN: [0-9]{8}"), fpe);

    }

    @Test
    public void cueWordOptionsAreReadFromThePolicy() throws Exception {

        final String policy = """
                {
                  "identifiers": {
                    "medicalRecordNumber": { "cueWords": [ "patient ID" ], "defaultCueWords": false }
                  }
                }
                """;

        Assertions.assertEquals("Patient ID: {{{REDACTED-medical-record-number}}}", filter(policy, "Patient ID: 00482913"));
        Assertions.assertEquals("MRN: 00482913", filter(policy, "MRN: 00482913"));

    }

    @Test
    public void theDocumentedExamplePolicyWorks() throws Exception {

        // The example on the filter's docs page.
        final String policy = """
                {
                   "identifiers": {
                      "medicalRecordNumber": {
                         "patterns": [
                            "\\\\b[0-9]{8}\\\\b",
                            "\\\\bMR-[0-9]{6}\\\\b"
                         ],
                         "cueWords": [
                            "patient ID"
                         ],
                         "medicalRecordNumberFilterStrategies": [
                            {
                               "strategy": "REDACT"
                            }
                         ]
                      }
                   }
                }
                """;

        Assertions.assertEquals("Patient ID: {{{REDACTED-medical-record-number}}}", filter(policy, "Patient ID: 00482913"));
        Assertions.assertEquals("MRN {{{REDACTED-medical-record-number}}}", filter(policy, "MRN MR-104422"));
        Assertions.assertEquals("MRN A1234567", filter(policy, "MRN A1234567"));

    }

    @Test
    public void compilesFromPhiSql() throws Exception {

        final Policy policy = Policy.fromPhiSQL("REDACT MEDICAL_RECORD_NUMBER WITH LAST_4 OPTIONS (cueWords = ['patient ID']);");
        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);

        Assertions.assertEquals("MRN: 2913", service.filter(policy, "context", "MRN: 00482913").getFilteredText());
        Assertions.assertEquals("Patient ID: 2913", service.filter(policy, "context", "Patient ID: 00482913").getFilteredText());

    }

    @Test
    public void anInvalidPatternRefusesThePolicy() {

        final String policy = """
                { "identifiers": { "medicalRecordNumber": { "patterns": [ "[A-Z" ] } } }
                """;

        Assertions.assertThrows(InvalidPolicyPatternException.class, () -> filter(policy, "MRN: 00482913"));

    }

}
