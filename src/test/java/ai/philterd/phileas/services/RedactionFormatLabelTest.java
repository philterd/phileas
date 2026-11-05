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
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.mockito.Mockito.mock;

/**
 * The %l redaction placeholder is replaced with the classification of the filter or pattern that
 * found the text, and with nothing when there is none.
 */
class RedactionFormatLabelTest {

    private static final String REDACT_WITH_LABEL =
            "[ { \"strategy\": \"REDACT\", \"redactionFormat\": \"{{{REDACTED-%t-%l}}}\" } ]";

    private final VectorService vectorService = mock(VectorService.class);

    @Test
    void anIdentifierClassificationIsTheLabel() throws Exception {

        final String json = "{ \"identifiers\": { \"identifiers\": [ "
                + identifier("MRN [0-9]{6,10}", "\"classification\": \"mrn\", ") + " ] } }";

        Assertions.assertEquals("Record {{{REDACTED-id-mrn}}} on file.",
                filter(json, "Record MRN 00482913 on file."));

    }

    @Test
    void identifiersWithDifferentClassificationsCanBeToldApart() throws Exception {

        final String json = "{ \"identifiers\": { \"identifiers\": [ "
                + identifier("MRN [0-9]{6,10}", "\"classification\": \"mrn\", ") + ", "
                + identifier("ACCT-[0-9]{6}", "\"classification\": \"account\", ") + " ] } }";

        Assertions.assertEquals("Record {{{REDACTED-id-mrn}}} and {{{REDACTED-id-account}}}.",
                filter(json, "Record MRN 00482913 and ACCT-123456."));

    }

    @Test
    void anIdentifierWithoutAClassificationUsesTheDefaultOne() throws Exception {

        final String json = "{ \"identifiers\": { \"identifiers\": [ " + identifier("MRN [0-9]{6,10}", "") + " ] } }";

        Assertions.assertEquals("Record {{{REDACTED-id-custom-identifier}}} on file.",
                filter(json, "Record MRN 00482913 on file."));

    }

    @Test
    void anEmptyClassificationLeavesTheLabelEmpty() throws Exception {

        final String json = "{ \"identifiers\": { \"identifiers\": [ "
                + identifier("MRN [0-9]{6,10}", "\"classification\": \"\", ") + " ] } }";

        Assertions.assertEquals("Record {{{REDACTED-id-}}} on file.", filter(json, "Record MRN 00482913 on file."));

    }

    @Test
    void aFilterWithoutAClassificationHasAnEmptyLabel() throws Exception {

        final String json = "{ \"identifiers\": { \"ssn\": { \"ssnFilterStrategies\": " + REDACT_WITH_LABEL + " } } }";

        Assertions.assertEquals("SSN {{{REDACTED-ssn-}}} on file.", filter(json, "SSN 078-05-1120 on file."));

    }

    @Test
    void aTrackingNumberCarrierIsTheLabel() throws Exception {

        final String json = "{ \"identifiers\": { \"trackingNumber\": { \"trackingNumberFilterStrategies\": " + REDACT_WITH_LABEL + " } } }";

        Assertions.assertEquals("Ship {{{REDACTED-tracking-number-ups}}} today.", filter(json, "Ship 1Z999AA10123456784 today."));

    }

    @Test
    void aPassportNumberCountryIsTheLabel() throws Exception {

        final String json = "{ \"identifiers\": { \"passportNumber\": { \"passportNumberFilterStrategies\": " + REDACT_WITH_LABEL + " } } }";

        Assertions.assertEquals("Passport number {{{REDACTED-passport-number-US}}} on file.",
                filter(json, "Passport number 123456789 on file."));

    }

    @Test
    void aDriversLicenseStateIsNotTheLabel() throws Exception {

        final String json = "{ \"identifiers\": { \"driversLicense\": { \"driversLicenseFilterStrategies\": " + REDACT_WITH_LABEL + " } } }";

        Assertions.assertEquals("License {{{REDACTED-drivers-license-number-}}} on file.",
                filter(json, "License A1234567 on file."));

    }

    private static String identifier(final String pattern, final String classification) {
        return "{ \"pattern\": \"" + pattern + "\", \"caseSensitive\": false, " + classification
                + "\"enabled\": true, \"identifierFilterStrategies\": " + REDACT_WITH_LABEL + " }";
    }

    private String filter(final String json, final String input) throws Exception {

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), new DefaultContextService(), vectorService, null);

        return service.filter(new Gson().fromJson(json, Policy.class), "context", input).getFilteredText();

    }

}
