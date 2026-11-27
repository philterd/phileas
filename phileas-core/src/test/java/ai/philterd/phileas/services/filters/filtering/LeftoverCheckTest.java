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
package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.LeftoverCheckResult;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.PolicySchema;
import ai.philterd.phileas.policy.config.LeftoverCheck;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LeftoverCheckTest {

    private static final String INPUT = "My email is test@example.com and my SSN is 123-45-6789.";

    private static final FilterOptions REQUESTED = FilterOptions.defaults().withLeftoverCheck(true);

    private static Policy policy(final String identifiers, final String leftoverCheck) {
        return new Gson().fromJson(policyJson(identifiers, leftoverCheck), Policy.class);
    }

    private static String policyJson(final String identifiers, final String leftoverCheck) {
        return "{\"identifiers\":" + identifiers
                + (leftoverCheck == null ? "" : ",\"config\":{\"review\":{\"leftoverCheck\":" + leftoverCheck + "}}") + "}";
    }

    private static PlainTextFilterService service() {
        return new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
    }

    private static List<FilterType> types(final LeftoverCheckResult result) {
        return result.getFindings().stream().map(LeftoverFinding::getFilterType).toList();
    }

    // --- configuration ---------------------------------------------------------------------

    @Test
    public void defaultsWhenAbsent() {
        for (final String json : new String[]{"{}", "{\"config\":{}}", "{\"config\":{\"review\":{}}}"}) {
            final Policy policy = new Gson().fromJson(json, Policy.class);
            Assertions.assertEquals(LeftoverCheck.Mode.OFF, policy.getConfig().getReview().getLeftoverCheck().getMode());
            Assertions.assertEquals(LeftoverCheck.Using.BROAD, policy.getConfig().getReview().getLeftoverCheck().getUsing());
        }
        final LeftoverCheck partial = policy("{}", "{\"mode\":\"always\"}").getConfig().getReview().getLeftoverCheck();
        Assertions.assertEquals(LeftoverCheck.Mode.ALWAYS, partial.getMode());
        Assertions.assertEquals(LeftoverCheck.Using.BROAD, partial.getUsing());
    }

    @Test
    public void roundTrips() {
        final Policy policy = policy("{}", "{\"mode\":\"onRequest\",\"using\":\"samePolicy\"}");
        final LeftoverCheck leftoverCheck = policy.getConfig().getReview().getLeftoverCheck();
        Assertions.assertEquals(LeftoverCheck.Mode.ON_REQUEST, leftoverCheck.getMode());
        Assertions.assertEquals(LeftoverCheck.Using.SAME_POLICY, leftoverCheck.getUsing());

        final JsonObject json = new Gson().toJsonTree(policy).getAsJsonObject()
                .getAsJsonObject("config").getAsJsonObject("review").getAsJsonObject("leftoverCheck");
        Assertions.assertEquals("onRequest", json.get("mode").getAsString());
        Assertions.assertEquals("samePolicy", json.get("using").getAsString());

        final Policy again = new Gson().fromJson(new Gson().toJson(policy), Policy.class);
        Assertions.assertEquals(LeftoverCheck.Mode.ON_REQUEST, again.getConfig().getReview().getLeftoverCheck().getMode());
        Assertions.assertEquals(LeftoverCheck.Using.SAME_POLICY, again.getConfig().getReview().getLeftoverCheck().getUsing());
    }

    @Test
    public void invalidSettingsAreLoadErrors() {
        for (final String invalid : new String[]{
                "\"always\"", "true", "[]",
                "{\"mode\":\"always\",\"extra\":1}",
                "{\"mode\":\"sometimes\"}", "{\"mode\":\"Always\"}", "{\"mode\":1}",
                "{\"using\":\"everything\"}", "{\"using\":\"BROAD\"}", "{\"using\":null}"}) {
            Assertions.assertThrows(JsonParseException.class, () -> policy("{}", invalid), invalid);
        }
    }

    @Test
    public void compiledFromPhiSQL() {
        final Policy policy = Policy.fromPhiSQL("CONFIGURE REVIEW (leftoverCheck = (mode = 'onRequest', using = 'samePolicy'));");
        Assertions.assertEquals(LeftoverCheck.Mode.ON_REQUEST, policy.getConfig().getReview().getLeftoverCheck().getMode());
        Assertions.assertEquals(LeftoverCheck.Using.SAME_POLICY, policy.getConfig().getReview().getLeftoverCheck().getUsing());
    }

    @Test
    public void schemaAcceptsLeftoverCheck() {
        Assertions.assertTrue(PolicySchema.validate(policyJson("{}", "{\"mode\":\"always\",\"using\":\"samePolicy\"}")));
        Assertions.assertFalse(PolicySchema.validate(policyJson("{}", "{\"mode\":\"sometimes\"}")));
    }

    // --- when it runs ------------------------------------------------------------------------

    @Test
    public void offNeverRuns() throws Exception {
        for (final String leftoverCheck : new String[]{null, "{\"mode\":\"off\"}"}) {
            final Policy policy = policy("{\"emailAddress\":{}}", leftoverCheck);
            for (final FilterOptions options : new FilterOptions[]{FilterOptions.defaults(), REQUESTED}) {
                final LeftoverCheckResult result = service().filter(policy, "context", INPUT, options).getLeftoverCheck();
                Assertions.assertFalse(result.isPerformed());
                Assertions.assertEquals(LeftoverCheckResult.Reason.NOT_REQUESTED, result.getReason());
            }
        }
    }

    @Test
    public void alwaysRunsWithoutBeingAsked() throws Exception {
        final Policy policy = policy("{\"emailAddress\":{}}", "{\"mode\":\"always\"}");
        final PlainTextFilterService service = service();
        Assertions.assertTrue(service.filter(policy, "context", INPUT).getLeftoverCheck().isPerformed());
        Assertions.assertTrue(service.filter(policy, "context", INPUT, FilterOptions.defaults().withLeftoverCheck(false))
                .getLeftoverCheck().isPerformed());
        Assertions.assertTrue(service.prepare(policy).filter("context", INPUT).getLeftoverCheck().isPerformed());
    }

    @Test
    public void onRequestRunsOnlyWhenAsked() throws Exception {
        final Policy policy = policy("{\"emailAddress\":{}}", "{\"mode\":\"onRequest\"}");
        final PlainTextFilterService service = service();

        Assertions.assertFalse(service.filter(policy, "context", INPUT).getLeftoverCheck().isPerformed());
        Assertions.assertTrue(service.filter(policy, "context", INPUT, REQUESTED).getLeftoverCheck().isPerformed());
        Assertions.assertTrue(service.filter(policy, new DefaultContextService(), new InMemoryVectorService(),
                "context", INPUT, REQUESTED).getLeftoverCheck().isPerformed());
        Assertions.assertTrue(service.prepare(policy).filter("context", INPUT, REQUESTED).getLeftoverCheck().isPerformed());
        Assertions.assertTrue(service.prepare(policy).filter(new DefaultContextService(), new InMemoryVectorService(),
                "context", INPUT, REQUESTED).getLeftoverCheck().isPerformed());
    }

    // --- what it finds -----------------------------------------------------------------------

    @Test
    public void broadFindsATypeThePolicyDoesNotInclude() throws Exception {
        final TextFilterResult broad = service().filter(policy("{\"emailAddress\":{}}",
                "{\"mode\":\"always\",\"using\":\"broad\"}"), "context", INPUT);
        final TextFilterResult samePolicy = service().filter(policy("{\"emailAddress\":{}}",
                "{\"mode\":\"always\",\"using\":\"samePolicy\"}"), "context", INPUT);

        Assertions.assertEquals("My email is {{{REDACTED-email-address}}} and my SSN is 123-45-6789.", broad.getFilteredText());

        final LeftoverCheckResult broadCheck = broad.getLeftoverCheck();
        Assertions.assertTrue(broadCheck.isPerformed());
        Assertions.assertNull(broadCheck.getReason());
        // Only the SSN: "My" and "and" are not reported as names, since broad does not add the name dictionaries.
        Assertions.assertEquals(List.of(FilterType.SSN), types(broadCheck), broadCheck.getFindings().toString());

        final LeftoverFinding ssn = broadCheck.getFindings().stream()
                .filter(finding -> finding.getFilterType() == FilterType.SSN).findFirst().orElseThrow();
        final int start = broad.getFilteredText().indexOf("123-45-6789");
        Assertions.assertEquals(start, ssn.getCharacterStart());
        Assertions.assertEquals(start + 11, ssn.getCharacterEnd());
        Assertions.assertTrue(ssn.getConfidence() > 0);
        Assertions.assertEquals(broadCheck.getFindings().size(), broadCheck.getFindingCount());

        Assertions.assertTrue(samePolicy.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(0, samePolicy.getLeftoverCheck().getFindingCount());
    }

    @Test
    public void runsThePolicysOwnConfiguredFilters() throws Exception {
        // Each filter's own ignore list leaves its value unredacted, so the check finds it with the filter as
        // configured: a custom identifier and a dictionary are not built-ins, so only the policy defines them.
        final String identifiers = "{"
                + "\"identifiers\":[{\"pattern\":\"CASE-[0-9]{4}\",\"classification\":\"case-number\",\"ignored\":[\"CASE-1234\"]}],"
                + "\"dictionaries\":[{\"terms\":[\"zanzibar\"],\"classification\":\"places\",\"ignored\":[\"zanzibar\"]}]}";
        final String input = "Filed as CASE-1234 in Zanzibar.";

        for (final String using : new String[]{"broad", "samePolicy"}) {
            final TextFilterResult result = service().filter(policy(identifiers,
                    "{\"mode\":\"always\",\"using\":\"" + using + "\"}"), "context", input);
            Assertions.assertEquals(input, result.getFilteredText(), using);
            final List<String> classifications = result.getLeftoverCheck().getFindings().stream()
                    .map(LeftoverFinding::getClassification).toList();
            Assertions.assertTrue(classifications.contains("case-number"), using + " " + classifications);
            Assertions.assertTrue(classifications.contains("places"), using + " " + classifications);
        }
    }

    @Test
    public void aStrategyConditionDoesNotHideAValue() throws Exception {
        final Policy policy = policy("{\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"REDACT\",\"condition\":\"token == \\\"000-00-0000\\\"\"}]}}",
                "{\"mode\":\"always\",\"using\":\"samePolicy\"}");
        final TextFilterResult result = service().filter(policy, "context", INPUT);
        Assertions.assertEquals(INPUT, result.getFilteredText());
        Assertions.assertEquals(List.of(FilterType.SSN), types(result.getLeftoverCheck()));
    }

    @Test
    public void insertedValuesAreNotReported() throws Exception {
        // Random stand-ins look like real values on purpose, and a redaction label holds text a filter can match.
        final String identifiers = "{\"emailAddress\":{\"emailAddressFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]},"
                + "\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]},"
                + "\"identifiers\":[{\"pattern\":\"[A-Z]{3,}\",\"classification\":\"upper\"}]}";
        final TextFilterResult result = service().filter(policy(identifiers, "{\"mode\":\"always\",\"using\":\"samePolicy\"}"),
                "context", "My email is test@example.com and my SSN is 123-45-6789 at ACME.");

        Assertions.assertFalse(result.getFilteredText().contains("test@example.com"));
        Assertions.assertFalse(result.getFilteredText().contains("123-45-6789"));
        Assertions.assertTrue(result.getFilteredText().contains("{{{REDACTED-id}}}"), result.getFilteredText());
        Assertions.assertTrue(result.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(0, result.getLeftoverCheck().getFindingCount(), result.getLeftoverCheck().getFindings().toString());
    }

    // A PhEye service that finds "Ann" in PHEYE_INPUT, labeled Person.
    private static final String PHEYE_INPUT = "Ann was seen today.";

    private static HttpClient phEyeFindingAnn() throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(new StringEntity(
                    "[{\"start\": 0, \"end\": 3, \"label\": \"Person\", \"text\": \"Ann\", \"score\": 0.9}]"));
            return handler.handleResponse(response);
        });
        return httpClient;
    }

    @Test
    public void runsThePolicysPhEyeFiltersAsConfigured() throws Exception {

        // Each filter's own ignore list leaves "Ann" unredacted, so only the check, which runs the filter without
        // its ignore list, can report it.
        final String endpoint = "\"phEyeConfiguration\":{\"endpoint\":\"http://localhost:18080\",\"labels\":[\"Person\"]},"
                + "\"ignored\":[\"Ann\"]";

        for (final String identifiers : new String[]{"{\"pheyes\":[{" + endpoint + "}]}", "{\"person\":{" + endpoint + "}}"}) {
            for (final String using : new String[]{"broad", "samePolicy"}) {

                final HttpClient httpClient = phEyeFindingAnn();
                final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                        new DefaultContextService(), new InMemoryVectorService(), httpClient);

                final TextFilterResult result = service.filter(policy(identifiers,
                        "{\"mode\":\"always\",\"using\":\"" + using + "\"}"), "context", PHEYE_INPUT);

                Assertions.assertEquals(PHEYE_INPUT, result.getFilteredText(), identifiers);
                final LeftoverFinding ann = result.getLeftoverCheck().getFindings().stream()
                        .filter(f -> "Person".equals(f.getClassification())).findFirst()
                        .orElseThrow(() -> new AssertionError(identifiers + " " + using + " " + result.getLeftoverCheck().getFindings()));
                Assertions.assertEquals(0, ann.getCharacterStart());
                Assertions.assertEquals(3, ann.getCharacterEnd());

            }
        }

    }

    @Test
    public void broadDoesNotAddAPhEyeFilter() throws Exception {
        final HttpClient httpClient = phEyeFindingAnn();
        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), httpClient);

        final String disabled = "{\"emailAddress\":{},\"pheyes\":[{\"enabled\":false,"
                + "\"phEyeConfiguration\":{\"endpoint\":\"http://localhost:18080\"}}]}";
        for (final String identifiers : new String[]{"{\"emailAddress\":{}}", disabled}) {
            final TextFilterResult result = service.filter(policy(identifiers, "{\"mode\":\"always\",\"using\":\"broad\"}"),
                    "context", PHEYE_INPUT);
            Assertions.assertTrue(result.getLeftoverCheck().isPerformed(), identifiers);
        }

        verify(httpClient, never()).execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any());
    }

    @Test
    public void topLevelIgnoreListsSuppressAFinding() throws Exception {
        final String leftoverCheck = "{\"mode\":\"always\",\"using\":\"broad\"}";

        final Policy ignored = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}},"
                + "\"ignored\":[{\"terms\":[\"123-45-6789\"]}],"
                + "\"config\":{\"review\":{\"leftoverCheck\":" + leftoverCheck + "}}}", Policy.class);
        Assertions.assertFalse(types(service().filter(ignored, "context", INPUT).getLeftoverCheck()).contains(FilterType.SSN));

        final Policy ignoredPattern = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{}},"
                + "\"ignoredPatterns\":[{\"pattern\":\"[0-9]{3}-[0-9]{2}-[0-9]{4}\"}],"
                + "\"config\":{\"review\":{\"leftoverCheck\":" + leftoverCheck + "}}}", Policy.class);
        Assertions.assertFalse(types(service().filter(ignoredPattern, "context", INPUT).getLeftoverCheck()).contains(FilterType.SSN));
    }

    @Test
    public void aFiltersOwnIgnoreListDoesNotSuppressAFinding() throws Exception {
        for (final String ignore : new String[]{"\"ignored\":[\"123-45-6789\"]",
                "\"ignoredPatterns\":[{\"pattern\":\"[0-9]{3}-[0-9]{2}-[0-9]{4}\"}]"}) {
            final Policy policy = policy("{\"ssn\":{" + ignore + "}}", "{\"mode\":\"always\",\"using\":\"samePolicy\"}");
            final TextFilterResult result = service().filter(policy, "context", INPUT);
            Assertions.assertEquals(INPUT, result.getFilteredText(), ignore);
            Assertions.assertEquals(List.of(FilterType.SSN), types(result.getLeftoverCheck()), ignore);
        }
    }

    @Test
    public void findsAValueAcrossAChunkBoundary() throws Exception {
        // Wrapped at 20 characters with no overlap, the pieces are "His number is 123 45" and "6789 on file.", so
        // the redaction does not see the SSN whole.
        final String input = "His number is 123 45 6789 on file.";

        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"ssn\":{}},"
                + "\"config\":{\"splitting\":{\"enabled\":true,\"threshold\":20,\"method\":\"width\"},"
                + "\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"samePolicy\"}}}}", Policy.class);
        final TextFilterResult result = service().filter(policy, "context", input);

        Assertions.assertEquals(input, result.getFilteredText());
        final LeftoverFinding finding = result.getLeftoverCheck().getFindings().stream()
                .filter(f -> f.getFilterType() == FilterType.SSN).findFirst().orElseThrow();
        Assertions.assertEquals("123 45 6789",
                result.getFilteredText().substring(finding.getCharacterStart(), finding.getCharacterEnd()));
    }

    @Test
    public void insertedValuesAreNotReportedAcrossPieces() throws Exception {
        final String input = "SSN 123-45-6789 here.\nSSN 234-56-7890 there.";
        final Policy policy = new Gson().fromJson("{\"identifiers\":{\"ssn\":{\"ssnFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]}},"
                + "\"config\":{\"splitting\":{\"enabled\":true,\"threshold\":10,\"method\":\"newline\"},"
                + "\"review\":{\"leftoverCheck\":{\"mode\":\"always\",\"using\":\"samePolicy\"}}}}", Policy.class);
        final TextFilterResult result = service().filter(policy, "context", input);

        Assertions.assertFalse(result.getFilteredText().contains("123-45-6789"));
        Assertions.assertFalse(result.getFilteredText().contains("234-56-7890"));
        Assertions.assertEquals(0, result.getLeftoverCheck().getFindingCount(), result.getLeftoverCheck().getFindings().toString());
    }

    // --- the result --------------------------------------------------------------------------

    @Test
    public void matchedTextOnlyWhenAskedAndNeverSerialized() throws Exception {
        final Policy policy = policy("{\"emailAddress\":{}}", "{\"mode\":\"always\",\"using\":\"broad\"}");

        final TextFilterResult withoutText = service().filter(policy, "context", INPUT);
        Assertions.assertTrue(withoutText.getLeftoverCheck().getFindingCount() > 0);
        withoutText.getLeftoverCheck().getFindings().forEach(finding -> Assertions.assertNull(finding.getMatchedText()));

        final TextFilterResult withText = service().filter(policy, "context", INPUT,
                FilterOptions.defaults().withLeftoverCheckMatchedText(true));
        final LeftoverFinding ssn = withText.getLeftoverCheck().getFindings().stream()
                .filter(finding -> finding.getFilterType() == FilterType.SSN).findFirst().orElseThrow();
        Assertions.assertEquals("123-45-6789", ssn.getMatchedText());

        final JsonObject leftoverCheck = new Gson().toJsonTree(withText).getAsJsonObject().getAsJsonObject("leftoverCheck");
        Assertions.assertFalse(leftoverCheck.toString().contains("123-45-6789"), leftoverCheck.toString());
        Assertions.assertFalse(leftoverCheck.toString().contains("matchedText"), leftoverCheck.toString());
        Assertions.assertTrue(leftoverCheck.get("performed").getAsBoolean());
        Assertions.assertEquals(withText.getLeftoverCheck().getFindingCount(), leftoverCheck.get("findingCount").getAsInt());
        Assertions.assertEquals("SSN", leftoverCheck.getAsJsonArray("findings").asList().stream()
                .map(e -> e.getAsJsonObject().get("filterType").getAsString())
                .filter("SSN"::equals).findFirst().orElseThrow());
    }

    @Test
    public void aTextFindingSerializesWithoutTheFieldsOfOtherFormats() throws Exception {
        final TextFilterResult result = service().filter(policy("{\"emailAddress\":{}}",
                "{\"mode\":\"always\",\"using\":\"broad\"}"), "context", INPUT);
        final JsonObject leftoverCheck = new Gson().toJsonTree(result).getAsJsonObject().getAsJsonObject("leftoverCheck");
        Assertions.assertFalse(leftoverCheck.has("reason"), leftoverCheck.toString());
        Assertions.assertFalse(leftoverCheck.has("uncheckedAttachments"), leftoverCheck.toString());
        final JsonObject finding = leftoverCheck.getAsJsonArray("findings").get(0).getAsJsonObject();
        for (final String absent : new String[]{"row", "column", "attachment"}) {
            Assertions.assertFalse(finding.has(absent), absent + " in " + finding);
        }
    }

    @Test
    public void aMatchTheFiltersOwnSettingsRejectIsNotReported() throws Exception {
        // With validation, a zip code not in the zip code database is found but not applied, so the redaction
        // leaves it and the check, which runs the filter as configured, does not report it.
        final String input = "Ship to 99999 or 90210.";
        final TextFilterResult result = service().filter(policy("{\"zipCode\":{\"validate\":true}}",
                "{\"mode\":\"always\",\"using\":\"samePolicy\"}"), "context", input);
        Assertions.assertTrue(result.getFilteredText().contains("99999"), result.getFilteredText());
        Assertions.assertFalse(result.getFilteredText().contains("90210"), result.getFilteredText());
        Assertions.assertTrue(result.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(0, result.getLeftoverCheck().getFindingCount(), result.getLeftoverCheck().getFindings().toString());
    }

    @Test
    public void aCheckThatFailsIsReportedAsNotPerformedAndTheRedactionStands() throws Exception {
        final Policy policy = policy("{\"emailAddress\":{}}", "{\"mode\":\"always\",\"using\":\"broad\"}");
        final PlainTextFilterService failing = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null) {
            @Override
            protected List<Span> leftoverSpans(final Policy p, final LeftoverCheck.Using using, final String text) {
                throw new IllegalStateException("check failed");
            }
        };

        final TextFilterResult result = failing.filter(policy, "context", INPUT);

        Assertions.assertEquals(service().filter(policy, "context", INPUT).getFilteredText(), result.getFilteredText());
        Assertions.assertFalse(result.getLeftoverCheck().isPerformed());
        Assertions.assertEquals(LeftoverCheckResult.Reason.CHECK_FAILED, result.getLeftoverCheck().getReason());
        Assertions.assertEquals(0, result.getLeftoverCheck().getFindingCount());

        final JsonObject json = new Gson().toJsonTree(result).getAsJsonObject().getAsJsonObject("leftoverCheck");
        Assertions.assertFalse(json.get("performed").getAsBoolean(), json.toString());
        Assertions.assertEquals("checkFailed", json.get("reason").getAsString(), json.toString());
    }

    @Test
    public void anInterruptedCheckKeepsTheThreadsInterruptFlag() throws Exception {
        final Policy policy = policy("{\"emailAddress\":{}}", "{\"mode\":\"always\",\"using\":\"broad\"}");
        final PlainTextFilterService interrupted = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null) {
            @Override
            protected List<Span> leftoverSpans(final Policy p, final LeftoverCheck.Using using, final String text)
                    throws InterruptedException {
                throw new InterruptedException();
            }
        };

        final TextFilterResult result = interrupted.filter(policy, "context", INPUT);

        // Thread.interrupted() also clears the flag for the tests that follow.
        Assertions.assertTrue(Thread.interrupted());
        Assertions.assertEquals(LeftoverCheckResult.Reason.CHECK_FAILED, result.getLeftoverCheck().getReason());
    }

    @Test
    public void resultWithoutTheCheckSerializesAsBefore() throws Exception {
        final TextFilterResult result = service().filter(policy("{\"emailAddress\":{}}", null), "context", INPUT);
        Assertions.assertFalse(new Gson().toJsonTree(result).getAsJsonObject().has("leftoverCheck"));
    }

    @Test
    public void checkDoesNotChangeOutput() throws Exception {
        final String identifiers = "{\"emailAddress\":{},\"ssn\":{\"ignored\":[\"123-45-6789\"]}}";
        final TextFilterResult off = service().filter(policy(identifiers, null), "context", INPUT);

        for (final String using : new String[]{"broad", "samePolicy"}) {
            final TextFilterResult checked = service().filter(policy(identifiers,
                    "{\"mode\":\"always\",\"using\":\"" + using + "\"}"), "context", INPUT);
            Assertions.assertTrue(checked.getLeftoverCheck().getFindingCount() > 0, using);
            Assertions.assertEquals(off.getFilteredText(), checked.getFilteredText(), using);
            Assertions.assertEquals(off.getExplanation(), checked.getExplanation(), using);
            Assertions.assertEquals(off.getTokens(), checked.getTokens(), using);
        }
    }

}
