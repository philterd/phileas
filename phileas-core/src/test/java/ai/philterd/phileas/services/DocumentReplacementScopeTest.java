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
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.CsvFilterService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocumentReplacementScopeTest {

    private static final String PHONE = "212-555-0187";
    private static final String ZIP = "90210";

    // Enough documents that independent random replacements cannot all be equal by chance.
    private static final int DOCUMENTS = 8;

    private static Policy policy(final String identifiers) {
        return new Gson().fromJson("{\"identifiers\":{" + identifiers + "}}", Policy.class);
    }

    private static String random(final String filter, final String strategies, final String scope) {
        return "\"" + filter + "\":{\"" + strategies + "\":[{\"strategy\":\"RANDOM_REPLACE\",\"replacementScope\":\"" + scope + "\"}]}";
    }

    private static PlainTextFilterService service(final ContextService contextService, final HttpClient httpClient) {
        return new PlainTextFilterService(new PhileasConfiguration(new Properties()), contextService,
                new InMemoryVectorService(), httpClient);
    }

    private static TextFilterResult filter(final PlainTextFilterService service, final ContextService contextService,
                                           final Policy policy, final String text) throws Exception {
        return service.filter(policy, contextService, new InMemoryVectorService(), "ctx", text, FilterOptions.defaults());
    }

    // The replacement of each span in a result, in order.
    private static List<String> replacements(final List<Span> spans) {
        final List<String> out = new ArrayList<>();
        for (final Span span : spans) {
            out.add(span.getReplacement());
        }
        return out;
    }

    @Test
    public void documentScopeGivesEveryOccurrenceOfAValueOneReplacement() throws Exception {

        final ContextService contextService = new DefaultContextService();
        final PlainTextFilterService service = service(contextService, null);
        final Policy policy = policy(random("phoneNumber", "phoneNumberFilterStrategies", "DOCUMENT") + ","
                + random("zipCode", "zipCodeFilterStrategies", "DOCUMENT"));

        final TextFilterResult result = filter(service, contextService, policy,
                "Call " + PHONE + " or " + PHONE + " at " + ZIP + ". Again " + PHONE + ", zip " + ZIP + ".");

        final Set<String> phones = new HashSet<>();
        final Set<String> zips = new HashSet<>();
        for (final Span span : result.getExplanation().appliedSpans()) {
            (span.getText().equals(PHONE) ? phones : zips).add(span.getReplacement());
        }
        Assertions.assertEquals(1, phones.size(), phones.toString());
        Assertions.assertEquals(1, zips.size(), zips.toString());
        Assertions.assertFalse(result.getFilteredText().contains(PHONE));

    }

    @Test
    public void documentScopeGivesEachDocumentItsOwnReplacementsInOneContext() throws Exception {

        final ContextService contextService = new DefaultContextService();
        final PlainTextFilterService service = service(contextService, null);
        final Policy policy = policy(random("phoneNumber", "phoneNumberFilterStrategies", "DOCUMENT"));

        final Set<String> perDocument = new HashSet<>();
        for (int i = 0; i < DOCUMENTS; i++) {
            perDocument.addAll(replacements(filter(service, contextService, policy, "Call " + PHONE + ".")
                    .getExplanation().appliedSpans()));
        }
        Assertions.assertTrue(perDocument.size() > 1, perDocument.toString());

    }

    @Test
    public void documentScopeNeverUsesTheCallersContext() throws Exception {

        final RecordingContextService contextService = new RecordingContextService();
        final PlainTextFilterService service = service(contextService, null);
        final Policy policy = policy(random("phoneNumber", "phoneNumberFilterStrategies", "DOCUMENT") + ","
                + "\"date\":{\"dateFilterStrategies\":[{\"strategy\":\"SHIFT\",\"shiftRandom\":true,\"replacementScope\":\"DOCUMENT\"}]}");

        filter(service, contextService, policy, "Call " + PHONE + " on 2024-03-05 and " + PHONE + " on 2024-03-09.");

        Assertions.assertEquals(List.of(), contextService.calls);

    }

    @Test
    public void contextScopeIsUnchanged() throws Exception {

        final RecordingContextService contextService = new RecordingContextService();
        final PlainTextFilterService service = service(contextService, null);
        final Policy policy = policy(random("phoneNumber", "phoneNumberFilterStrategies", "CONTEXT"));

        final Set<String> across = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            across.addAll(replacements(filter(service, contextService, policy, "Call " + PHONE + " or " + PHONE + ".")
                    .getExplanation().appliedSpans()));
        }
        Assertions.assertEquals(1, across.size(), across.toString());
        Assertions.assertTrue(contextService.calls.contains("computeReplacementIfAbsent " + PHONE), contextService.calls.toString());

    }

    @Test
    public void aRandomShiftMovesEveryDateOfADocumentByTheSameDays() throws Exception {

        final ContextService contextService = new DefaultContextService();
        final PlainTextFilterService service = service(contextService, null);
        final Policy policy = policy("\"date\":{\"dateFilterStrategies\":[{\"strategy\":\"SHIFT\",\"shiftRandom\":true}]}");

        final Set<Long> shifts = new HashSet<>();
        for (int i = 0; i < DOCUMENTS; i++) {
            final List<Span> spans = filter(service, contextService, policy,
                    "Seen 2024-03-05, again 2024-03-09, and 2024-07-20.").getExplanation().appliedSpans();
            Assertions.assertEquals(3, spans.size());
            final Set<Long> inDocument = new HashSet<>();
            for (final Span span : spans) {
                inDocument.add(ChronoUnit.DAYS.between(LocalDate.parse(span.getText()), LocalDate.parse(span.getReplacement())));
            }
            Assertions.assertEquals(1, inDocument.size(), inDocument.toString());
            shifts.addAll(inDocument);
        }
        // Independent between documents.
        Assertions.assertTrue(shifts.size() > 1, shifts.toString());

    }

    @Test
    public void aContextScopedShiftWithNoContextServiceStillShifts() throws Exception {

        // With no context service there is no context to keep the shift in, so each date is shifted on its own, as before.
        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                (HttpClient) null);
        final Policy policy = policy("\"date\":{\"dateFilterStrategies\":[{\"strategy\":\"SHIFT\",\"shiftRandom\":true,"
                + "\"replacementScope\":\"CONTEXT\"}]}");

        final List<Span> spans = service.filter(policy, null, new InMemoryVectorService(), "ctx", "Seen 2024-03-05.",
                FilterOptions.defaults()).getExplanation().appliedSpans();

        Assertions.assertEquals(1, spans.size());
        Assertions.assertNotEquals("2024-03-05", spans.get(0).getReplacement());

    }

    @Test
    public void mapReplaceAsksTheGeneratorOncePerValueInADocument() throws Exception {

        final AtomicInteger calls = new AtomicInteger();
        final HttpClient generator = mock(HttpClient.class);
        when(generator.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(new StringEntity("{\"response\": \"GENERATED" + calls.incrementAndGet() + "\"}"));
            return handler.handleResponse(response);
        });
        final Policy policy = new Gson().fromJson("{\"generators\":{\"g\":{\"type\":\"ollama\",\"endpoint\":\"http://localhost:11434\","
                + "\"model\":\"m\",\"prompt\":\"Replace {{token}}.\",\"timeoutMs\":1000}},\"identifiers\":{\"ssn\":{\"ssnFilterStrategies\":"
                + "[{\"strategy\":\"MAP_REPLACE\",\"generator\":\"g\",\"fallbackStrategy\":\"REDACT\"}]}}}", Policy.class);
        final ContextService contextService = new DefaultContextService();

        final TextFilterResult result = filter(service(contextService, generator), contextService, policy,
                "SSN 123-45-6789, confirmed 123-45-6789.");

        Assertions.assertEquals(List.of("GENERATED1", "GENERATED1"), replacements(result.getExplanation().appliedSpans()));
        Assertions.assertEquals(1, calls.get());

    }

    @Test
    public void pheyeNamesGetOneReplacementInADocument() throws Exception {

        final HttpClient pheye = mock(HttpClient.class);
        when(pheye.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final StringBuilder spans = new StringBuilder("[");
            if (invocation.getArgument(0) instanceof HttpPost post) {
                final String text = new Gson().fromJson(EntityUtils.toString(post.getEntity()), JsonObject.class).get("text").getAsString();
                final Matcher ann = Pattern.compile("Ann").matcher(text);
                while (ann.find()) {
                    spans.append(spans.length() > 1 ? "," : "").append("{\"start\": ").append(ann.start()).append(", \"end\": ")
                            .append(ann.end()).append(", \"label\": \"Person\", \"text\": \"Ann\", \"score\": 0.9}");
                }
            }
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(new StringEntity(spans.append("]").toString()));
            return handler.handleResponse(response);
        });
        final Policy policy = policy("\"pheyes\":[{\"phEyeConfiguration\":{\"endpoint\":\"http://localhost:18080\",\"labels\":[\"Person\"]},"
                + "\"phEyeFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\"}]}]");
        final ContextService contextService = new DefaultContextService();

        final List<String> names = replacements(filter(service(contextService, pheye), contextService, policy,
                "Ann called. Later Ann wrote, and Ann left.").getExplanation().appliedSpans());

        Assertions.assertEquals(3, names.size(), names.toString());
        Assertions.assertEquals(1, new HashSet<>(names).size(), names.toString());

    }

    @Test
    public void concurrentCallsOnAPreparedPolicyKeepTheirOwnMaps() throws Exception {

        final ContextService contextService = new DefaultContextService();
        final PlainTextFilterService.PreparedPolicy prepared = service(contextService, null)
                .prepare(policy(random("phoneNumber", "phoneNumberFilterStrategies", "DOCUMENT")));

        final ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            final List<Future<List<String>>> futures = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                futures.add(pool.submit(() -> replacements(prepared.filter(contextService, new InMemoryVectorService(), "ctx",
                        "Call " + PHONE + " or " + PHONE + ".", FilterOptions.defaults()).getExplanation().appliedSpans())));
            }
            final Set<String> all = new HashSet<>();
            for (final Future<List<String>> future : futures) {
                final List<String> inDocument = future.get();
                Assertions.assertEquals(1, new HashSet<>(inDocument).size(), inDocument.toString());
                all.addAll(inDocument);
            }
            Assertions.assertTrue(all.size() > 1, all.toString());
        } finally {
            pool.shutdownNow();
        }

    }

    @Test
    public void oneMapSpansEveryCellOfACsvDocument() throws Exception {

        final ContextService contextService = new DefaultContextService();
        final CsvFilterService service = new CsvFilterService(new PhileasConfiguration(new Properties()), contextService,
                new InMemoryVectorService(), null);
        final Policy policy = policy(random("phoneNumber", "phoneNumberFilterStrategies", "DOCUMENT"));

        final BinaryDocumentFilterResult result = service.filter(policy, contextService, new InMemoryVectorService(), "ctx",
                ("Home,Work\n" + PHONE + "," + PHONE + "\nnone," + PHONE + "\n").getBytes(StandardCharsets.UTF_8), MimeType.TEXT_CSV,
                FilterOptions.defaults());

        final List<String> phones = replacements(result.getExplanation().appliedSpans());
        Assertions.assertEquals(3, phones.size(), phones.toString());
        Assertions.assertEquals(1, new HashSet<>(phones).size(), phones.toString());

    }

    // Records every use of the caller's context service.
    private static final class RecordingContextService implements ContextService {

        private final DefaultContextService delegate = new DefaultContextService();
        private final List<String> calls = new ArrayList<>();

        @Override
        public synchronized boolean containsToken(final String token) {
            calls.add("containsToken " + token);
            return delegate.containsToken(token);
        }

        @Override
        public synchronized boolean containsReplacement(final String replacement) {
            calls.add("containsReplacement");
            return delegate.containsReplacement(replacement);
        }

        @Override
        public synchronized String getReplacement(final String token) {
            calls.add("getReplacement " + token);
            return delegate.getReplacement(token);
        }

        @Override
        public synchronized void putReplacement(final String token, final String replacement, final String filterType) {
            calls.add("putReplacement " + token);
            delegate.putReplacement(token, replacement, filterType);
        }

        @Override
        public synchronized String computeReplacementIfAbsent(final String token, final String filterType,
                                                              final Supplier<String> replacementSupplier) {
            calls.add("computeReplacementIfAbsent " + token);
            return delegate.computeReplacementIfAbsent(token, filterType, replacementSupplier);
        }

        @Override
        public synchronized String computeValueIfAbsent(final String key, final String filterType, final Supplier<String> valueSupplier) {
            calls.add("computeValueIfAbsent " + key);
            return delegate.computeValueIfAbsent(key, filterType, valueSupplier);
        }

    }

}
