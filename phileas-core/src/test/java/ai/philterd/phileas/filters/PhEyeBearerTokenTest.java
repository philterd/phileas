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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.services.filters.ai.pheye.PhEyeConfiguration;
import ai.philterd.phileas.services.filters.ai.pheye.RemotePhEyeDetector;
import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The PHEYE_BEARER_TOKEN default is sent only to the PHEYE_ENDPOINT endpoint (issue #444).
 */
@SetEnvironmentVariable(key = "PHEYE_ENDPOINT", value = "http://pheye.internal:5000/")
@SetEnvironmentVariable(key = "PHEYE_BEARER_TOKEN", value = "environment-token")
public class PhEyeBearerTokenTest {

    @Test
    public void environmentTokenNotSentToAnotherEndpoint() throws Exception {

        final HttpPost request = send("{\"endpoint\": \"http://other.example.com:5000\"}");

        Assertions.assertEquals("http://other.example.com:5000/find", request.getUri().toString());
        Assertions.assertNull(request.getFirstHeader("Authorization"));

    }

    @Test
    public void environmentTokenSentWhenEndpointLeftOut() throws Exception {

        final HttpPost request = send("{}");

        Assertions.assertEquals("pheye.internal", request.getUri().getHost());
        Assertions.assertEquals("Bearer environment-token", authorization(request));

    }

    @Test
    public void environmentTokenSentToSameEndpoint() throws Exception {

        final HttpPost request = send("{\"endpoint\": \"http://pheye.internal:5000/\"}");

        Assertions.assertEquals("Bearer environment-token", authorization(request));

    }

    @Test
    public void environmentTokenSentToSameEndpointWithoutTrailingSlash() throws Exception {

        final HttpPost request = send("{\"endpoint\": \"http://pheye.internal:5000\"}");

        Assertions.assertEquals("Bearer environment-token", authorization(request));

    }

    @Test
    public void policyTokenSentToAnotherEndpoint() throws Exception {

        final HttpPost request = send("{\"endpoint\": \"http://other.example.com:5000\", \"bearerToken\": \"policy-token\"}");

        Assertions.assertEquals("Bearer policy-token", authorization(request));

    }

    @Test
    public void policyTokenSentToSameEndpoint() throws Exception {

        final HttpPost request = send("{\"bearerToken\": \"policy-token\"}");

        Assertions.assertEquals("Bearer policy-token", authorization(request));

    }

    @Test
    public void policyTokenNotSerializedFromEnvironment() {

        final ai.philterd.phileas.policy.filters.pheye.PhEyeConfiguration policyConfiguration =
                new Gson().fromJson("{}", ai.philterd.phileas.policy.filters.pheye.PhEyeConfiguration.class);

        Assertions.assertFalse(new Gson().toJson(policyConfiguration).contains("environment-token"));

    }

    private static String authorization(final HttpPost request) {
        final Header header = request.getFirstHeader("Authorization");
        Assertions.assertNotNull(header);
        return header.getValue();
    }

    /**
     * Reads a policy's phEyeConfiguration, carries it into the detector's configuration as
     * FilterPolicyLoader does, and returns the request the detector sends.
     */
    private static HttpPost send(final String policyJson) throws Exception {

        final ai.philterd.phileas.policy.filters.pheye.PhEyeConfiguration policyConfiguration =
                new Gson().fromJson(policyJson, ai.philterd.phileas.policy.filters.pheye.PhEyeConfiguration.class);

        final PhEyeConfiguration phEyeConfiguration = new PhEyeConfiguration(policyConfiguration.getEndpoint());
        phEyeConfiguration.setTimeout(policyConfiguration.getTimeout());
        phEyeConfiguration.setBearerToken(policyConfiguration.getBearerToken());

        final HttpClient httpClient = mock(HttpClient.class);

        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(new StringEntity("[]"));
            return handler.handleResponse(response);
        });

        new RemotePhEyeDetector(phEyeConfiguration, httpClient).detect("George Washington", List.of("name"), "context", 0);

        final ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClient).execute(captor.capture(), ArgumentMatchers.<HttpClientResponseHandler<String>>any());

        return captor.getValue();

    }

}
