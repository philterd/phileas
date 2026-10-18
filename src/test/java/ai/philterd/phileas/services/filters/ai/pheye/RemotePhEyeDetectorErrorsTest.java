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
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.net.UnknownHostException;
import java.util.List;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RemotePhEyeDetectorErrorsTest {

    private static final String ENDPOINT = "http://philter-ph-eye-1:5000/";

    private static RemotePhEyeDetector detector(final HttpClient httpClient) {
        return new RemotePhEyeDetector(new PhEyeConfiguration(ENDPOINT), httpClient);
    }

    @Test
    public void aServiceThatCannotBeReachedIsUnavailable() throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        final UnknownHostException cause = new UnknownHostException("philter-ph-eye-1");
        when(httpClient.execute(isA(HttpPost.class), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenThrow(cause);

        final PhEyeUnavailableException e = Assertions.assertThrows(PhEyeUnavailableException.class,
                () -> detector(httpClient).detect("George Washington", List.of("name"), "c", 0));

        Assertions.assertEquals(ENDPOINT, e.getEndpoint());
        Assertions.assertSame(cause, e.getCause());
        Assertions.assertFalse(e.getMessage().contains("philter-ph-eye-1"), "the endpoint is not in the message");
    }

    @Test
    public void anErrorStatusIsUnavailable() throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(isA(HttpPost.class), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(500);
            when(response.getEntity()).thenReturn(new StringEntity("model failed"));
            return handler.handleResponse(response);
        });

        final PhEyeUnavailableException e = Assertions.assertThrows(PhEyeUnavailableException.class,
                () -> detector(httpClient).detect("George Washington", List.of("name"), "c", 0));

        Assertions.assertEquals(ENDPOINT, e.getEndpoint());
        Assertions.assertTrue(e.getMessage().contains("500"), e.getMessage());
    }

}
