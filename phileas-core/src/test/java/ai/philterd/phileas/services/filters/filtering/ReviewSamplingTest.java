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
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.PolicySchema;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class ReviewSamplingTest {

    private static final String INPUT = "My email is test@example.com and my SSN is 123-45-6789.";

    private static String policyJson(final String review) {
        return "{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}"
                + (review == null ? "" : ",\"config\":{\"review\":" + review + "}") + "}";
    }

    private static Policy policy(final String review) {
        return new Gson().fromJson(policyJson(review), Policy.class);
    }

    private static PlainTextFilterService service() {
        return new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
    }

    @Test
    public void samplingRateDefaultsToZero() {
        Assertions.assertEquals(0.0, policy(null).getConfig().getReview().getSamplingRate());
        Assertions.assertEquals(0.0, policy("{}").getConfig().getReview().getSamplingRate());
    }

    @Test
    public void samplingRateRoundTrips() {
        final Policy policy = policy("{\"samplingRate\":0.1}");
        Assertions.assertEquals(0.1, policy.getConfig().getReview().getSamplingRate());

        final JsonObject json = new Gson().toJsonTree(policy).getAsJsonObject();
        Assertions.assertEquals(0.1, json.getAsJsonObject("config").getAsJsonObject("review").get("samplingRate").getAsDouble());
        Assertions.assertEquals(0.1, new Gson().fromJson(json, Policy.class).getConfig().getReview().getSamplingRate());
    }

    @Test
    public void schemaAcceptsSamplingRate() {
        Assertions.assertTrue(PolicySchema.validate(policyJson("{\"samplingRate\":0.1}")));
        Assertions.assertFalse(PolicySchema.validate(policyJson("{\"samplingRate\":1.5}")));
    }

    @Test
    public void samplingRateOutsideZeroToOneIsALoadError() {
        final PlainTextFilterService service = service();
        for (final String rate : new String[]{"-0.01", "1.5"}) {
            final Policy policy = policy("{\"samplingRate\":" + rate + "}");
            final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> service.prepare(policy));
            Assertions.assertTrue(ex.getMessage().contains("samplingRate"), ex.getMessage());
            Assertions.assertThrows(IllegalArgumentException.class, () -> service.filter(policy, "context", INPUT));
        }
    }

    @Test
    public void notSampledByDefault() throws Exception {
        Assertions.assertFalse(service().filter(policy(null), "context", INPUT).isReviewSampled());
        Assertions.assertFalse(service().filter(policy("{\"samplingRate\":0.0}"), "context", INPUT).isReviewSampled());
    }

    @Test
    public void rateOfOneSelectsEveryRequest() throws Exception {
        final PlainTextFilterService service = service();
        final Policy policy = policy("{\"samplingRate\":1.0}");
        Assertions.assertTrue(service.filter(policy, "context", INPUT).isReviewSampled());
        Assertions.assertTrue(service.prepare(policy).filter("context", INPUT).isReviewSampled());
        Assertions.assertTrue(service.prepare(policy).filter(new DefaultContextService(), new InMemoryVectorService(),
                "context", INPUT).isReviewSampled());
    }

    @Test
    public void selectionSurvivesSplitting() throws Exception {
        // A small threshold splits the input into pieces, whose results are combined.
        for (final String method : new String[]{"newline", "characters"}) {
            final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}},"
                    + "\"config\":{\"review\":{\"samplingRate\":1.0},"
                    + "\"splitting\":{\"enabled\":true,\"threshold\":10,\"method\":\"" + method + "\"}}}", Policy.class);
            final TextFilterResult result = service().filter(policy, "context", INPUT + "\n" + INPUT);
            Assertions.assertTrue(result.isReviewSampled(), method);
        }
    }

    @Test
    public void selectionDoesNotChangeOutput() throws Exception {
        final TextFilterResult notSampled = service().filter(policy("{\"samplingRate\":0.0}"), "context", INPUT);
        final TextFilterResult sampled = service().filter(policy("{\"samplingRate\":1.0}"), "context", INPUT);

        Assertions.assertFalse(notSampled.isReviewSampled());
        Assertions.assertTrue(sampled.isReviewSampled());
        Assertions.assertEquals("My email is {{{REDACTED-email-address}}} and my SSN is {{{REDACTED-ssn}}}.", sampled.getFilteredText());
        Assertions.assertEquals(notSampled.getFilteredText(), sampled.getFilteredText());
        Assertions.assertEquals(notSampled.getExplanation(), sampled.getExplanation());
        Assertions.assertEquals(notSampled.getTokens(), sampled.getTokens());
        Assertions.assertEquals(notSampled.getContext(), sampled.getContext());
    }

    @Test
    public void drawnSelectionDoesNotChangeOutput() throws Exception {
        // Rates of 0.0 and 1.0 skip the draw, so an intermediate rate is what shows a draw leaves the output alone.
        final PlainTextFilterService service = service();
        final TextFilterResult baseline = service.filter(policy(null), "context", INPUT);
        final Policy policy = policy("{\"samplingRate\":0.5}");

        int sampled = 0;
        final int calls = 200;
        for (int i = 0; i < calls; i++) {
            final TextFilterResult result = service.filter(policy, "context", INPUT);
            if (result.isReviewSampled()) {
                sampled++;
            }
            Assertions.assertEquals(baseline.getFilteredText(), result.getFilteredText());
            Assertions.assertEquals(baseline.getExplanation(), result.getExplanation());
        }

        // Both outcomes occur; the chance either is missing in 200 calls at 0.5 is about 1 in 10^60.
        Assertions.assertTrue(sampled > 0 && sampled < calls, "sampled " + sampled + " of " + calls);
    }

    @Test
    public void resultJsonCarriesSelection() throws Exception {
        final TextFilterResult sampled = service().filter(policy("{\"samplingRate\":1.0}"), "context", INPUT);
        Assertions.assertTrue(new Gson().toJsonTree(sampled).getAsJsonObject().get("reviewSampled").getAsBoolean());
    }

}
