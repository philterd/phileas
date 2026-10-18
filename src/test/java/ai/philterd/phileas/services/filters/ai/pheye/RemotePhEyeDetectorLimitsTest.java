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
package ai.philterd.phileas.services.filters.ai.pheye;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RemotePhEyeDetectorLimitsTest {

    private static final int DEFAULT_WORDS = PhEyeDetector.DEFAULT_MAX_WORDS;

    private final AtomicLong now = new AtomicLong();

    /** An HTTP client that answers each request with the next status and body, repeating the last. */
    private static HttpClient answering(final AtomicInteger calls, final Object[]... answers) throws IOException {
        final HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<Object>>any())).thenAnswer(invocation -> {
            final Object[] answer = answers[Math.min(calls.getAndIncrement(), answers.length - 1)];
            if (answer[0] instanceof IOException e) {
                throw e;
            }
            final HttpClientResponseHandler<Object> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn((Integer) answer[0]);
            when(response.getEntity()).thenReturn(answer[1] == null ? null : new StringEntity((String) answer[1]));
            return handler.handleResponse(response);
        });
        return httpClient;
    }

    private static Object[] status(final int code, final String body) {
        return new Object[] {code, body};
    }

    private RemotePhEyeDetector detector(final HttpClient httpClient, final String bearerToken) {
        final PhEyeConfiguration configuration = new PhEyeConfiguration("http://pheye.internal:5000");
        configuration.setBearerToken(bearerToken);
        return new RemotePhEyeDetector(configuration, httpClient, now::get);
    }

    @Test
    public void readsTheLimitsFromModelOnceAndKeepsThem() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final HttpClient httpClient = answering(calls, status(200, "{\"name\":\"pii_en_small\",\"maxWords\":512,\"maxWidth\":12}"));
        final RemotePhEyeDetector detector = detector(httpClient, "secret");

        Assertions.assertEquals(512, detector.maxWords());
        Assertions.assertEquals(12, detector.maxWidth());
        Assertions.assertEquals(512, detector.maxWords());
        Assertions.assertEquals(1, calls.get());

        final ArgumentCaptor<ClassicHttpRequest> request = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        verify(httpClient).execute(request.capture(), ArgumentMatchers.<HttpClientResponseHandler<Object>>any());
        Assertions.assertEquals("GET", request.getValue().getMethod());
        Assertions.assertEquals("http://pheye.internal:5000/model", request.getValue().getUri().toString());
        Assertions.assertEquals("Bearer secret", request.getValue().getFirstHeader("Authorization").getValue());
        Assertions.assertEquals("application/json", request.getValue().getFirstHeader("Accept").getValue());
    }

    @Test
    public void sendsNoAuthorizationWithoutAToken() throws Exception {
        final HttpClient httpClient = answering(new AtomicInteger(), status(200, "{\"maxWords\":384,\"maxWidth\":12}"));
        Assertions.assertEquals(384, detector(httpClient, null).maxWords());

        final ArgumentCaptor<ClassicHttpRequest> request = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        verify(httpClient).execute(request.capture(), ArgumentMatchers.<HttpClientResponseHandler<Object>>any());
        Assertions.assertNull(request.getValue().getFirstHeader("Authorization"));
    }

    @Test
    public void anAnswerWithoutUsableLimitsIsKeptAsTheDefaults() throws Exception {
        // A service older than /model, one that refuses it, and bodies that are not the expected limits.
        final Object[][] answers = {status(404, "<html>Not Found</html>"), status(401, "unauthorized"),
                status(405, null), status(200, "{\"maxWords\":0,\"maxWidth\":12}"), status(200, "{\"maxWords\":512}"),
                status(200, "{\"maxWords\":-1,\"maxWidth\":-1}"), status(200, "[]"), status(200, "not json"),
                status(200, ""), status(200, null)};
        for (final Object[] answer : answers) {
            final AtomicInteger calls = new AtomicInteger();
            final RemotePhEyeDetector detector = detector(answering(calls, answer), null);
            Assertions.assertEquals(DEFAULT_WORDS, detector.maxWords(), answer[0] + " " + answer[1]);
            Assertions.assertEquals(PhEyeDetector.DEFAULT_MAX_WIDTH, detector.maxWidth());
            now.addAndGet(RemotePhEyeDetector.RETRY_NANOS * 2);
            detector.maxWords();
            Assertions.assertEquals(1, calls.get(), "asked again after " + answer[0] + " " + answer[1]);
        }
    }

    @Test
    public void aFailureThatMayPassUsesTheDefaultsAndIsAskedAgainAfterTheRetryInterval() throws Exception {
        for (final Object[] failure : new Object[][] {{new IOException("connection refused")}, status(503, "starting")}) {
            final AtomicInteger calls = new AtomicInteger();
            final RemotePhEyeDetector detector = detector(
                    answering(calls, failure, status(200, "{\"maxWords\":512,\"maxWidth\":12}")), null);

            Assertions.assertEquals(DEFAULT_WORDS, detector.maxWords());
            // Not asked again within the interval, however often the filter asks.
            now.addAndGet(RemotePhEyeDetector.RETRY_NANOS - 1);
            Assertions.assertEquals(DEFAULT_WORDS, detector.maxWords());
            Assertions.assertEquals(PhEyeDetector.DEFAULT_MAX_WIDTH, detector.maxWidth());
            Assertions.assertEquals(1, calls.get());
            // Asked again after it, and kept.
            now.addAndGet(1);
            Assertions.assertEquals(512, detector.maxWords());
            Assertions.assertEquals(512, detector.maxWords());
            Assertions.assertEquals(2, calls.get());
        }
    }

    @Test
    public void noHttpClientGetsTheDefaults() {
        Assertions.assertEquals(DEFAULT_WORDS, detector(null, null).maxWords());
    }

}
