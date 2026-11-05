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
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Ignored;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.InvalidPolicyPatternException;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.policy.filters.Identifier;
import ai.philterd.phileas.services.strategies.rules.IdentifierFilterStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ai.philterd.phileas.utils.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicy;

public class PlainTextFilterServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(PlainTextFilterServiceTest.class);

    private Gson gson;
    private final VectorService vectorService = Mockito.mock(VectorService.class);
    private final ContextService contextService = new DefaultContextService();

    @BeforeEach
    public void before() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(String.class, new PlaceholderDeserializer());
        gson = gsonBuilder.create();
    }

    @Test
    public void policyWithPlaceholder() throws IOException, URISyntaxException {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("john", "jeff", "${USER}"));

        final Policy policy = getPolicy();
        policy.setIgnored(List.of(ignored));
        final String json = gson.toJson(policy);
        LOGGER.info(json);

        final Policy deserialized = gson.fromJson(json, Policy.class);

        Assertions.assertEquals(3, policy.getIgnored().get(0).getTerms().size());
        Assertions.assertTrue(CollectionUtils.isNotEmpty(deserialized.getIgnored().get(0).getTerms()));

    }

    /**
     * An identifier pattern that overflows the stack is rejected when the policy is loaded, so it
     * fails on a document that contains nothing for it to match. The filter-level guard still covers
     * a pattern that only misbehaves on input the load-time probe does not resemble.
     */
    @Test
    public void stackOverflowInAnIdentifierPatternFailsThePolicy() throws Exception {

        final Policy policy = getPolicy();

        final Identifier identifier = new Identifier();
        identifier.setIdentifierFilterStrategies(List.of(new IdentifierFilterStrategy()));
        // A non-atomic repeated group recurses per character, so it overflows on a long input.
        identifier.setPattern("(?:[^\\s.]|\\.(?!\\s))*");
        identifier.setCaseSensitive(true);
        policy.getIdentifiers().setIdentifiers(List.of(identifier));

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), contextService, vectorService, null);

        final InvalidPolicyPatternException e = Assertions.assertThrows(InvalidPolicyPatternException.class,
                () -> service.filter(policy, "context", "short"));

        Assertions.assertTrue(e.getMessage().contains("overflowed the stack"), e.getMessage());

    }

}
