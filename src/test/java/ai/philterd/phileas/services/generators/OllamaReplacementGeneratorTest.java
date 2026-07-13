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
package ai.philterd.phileas.services.generators;

import ai.philterd.phileas.policy.Generator;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OllamaReplacementGeneratorTest {

    private Generator generator() {
        final Generator generator = new Generator();
        generator.setType(Generator.TYPE_OLLAMA);
        generator.setEndpoint("http://localhost:11434");
        generator.setModel("llama3.1");
        generator.setPrompt("Replace {{token}} labeled {{label}}.");
        generator.setTimeoutMs(2000);
        return generator;
    }

    @Test
    public void generatesReplacementFromResponseField() throws Exception {

        final HttpClient httpClient = mock(HttpClient.class);

        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            final HttpEntity entity = new StringEntity("{\"response\": \"  Widget Co  \"}");
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(entity);
            return handler.handleResponse(response);
        });

        final OllamaReplacementGenerator ollama = new OllamaReplacementGenerator(generator(), httpClient);

        // The trimmed 'response' field is returned.
        Assertions.assertEquals("Widget Co", ollama.generate("Acme Corp", "identifier"));

    }

    @Test
    public void templatesTokenAndLabelIntoPrompt() throws Exception {

        final HttpClient httpClient = mock(HttpClient.class);

        final ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);

        when(httpClient.execute(captor.capture(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(new StringEntity("{\"response\": \"x\"}"));
            return handler.handleResponse(response);
        });

        new OllamaReplacementGenerator(generator(), httpClient).generate("Acme Corp", "identifier");

        final String requestBody = EntityUtils.toString(captor.getValue().getEntity());
        Assertions.assertTrue(requestBody.contains("Replace Acme Corp labeled identifier."), requestBody);

    }

    @Test
    public void throwsOnNon200() throws Exception {

        final HttpClient httpClient = mock(HttpClient.class);

        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(500);
            when(response.getEntity()).thenReturn(new StringEntity(""));
            return handler.handleResponse(response);
        });

        final OllamaReplacementGenerator ollama = new OllamaReplacementGenerator(generator(), httpClient);

        Assertions.assertThrows(Exception.class, () -> ollama.generate("Acme Corp", "identifier"));

    }

    @Test
    public void throwsWhenResponseFieldMissing() throws Exception {

        final HttpClient httpClient = mock(HttpClient.class);

        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<String>>any())).thenAnswer(invocation -> {
            final HttpClientResponseHandler<String> handler = invocation.getArgument(1);
            final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            when(response.getCode()).thenReturn(200);
            when(response.getEntity()).thenReturn(new StringEntity("{\"done\": true}"));
            return handler.handleResponse(response);
        });

        final OllamaReplacementGenerator ollama = new OllamaReplacementGenerator(generator(), httpClient);

        Assertions.assertThrows(Exception.class, () -> ollama.generate("Acme Corp", "identifier"));

    }

}
