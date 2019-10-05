package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.profile.filters.strategies.rules.IdentifierFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;

public class IdentifierFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(IdentifierFilterStrategyTest.class);

    @Test
    public void replacement1() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final IdentifierFilterStrategy strategy = new IdentifierFilterStrategy();
        final String replacement = strategy.getReplacement("custom-name", "context", "documentId", "token", anonymizationService);

        Assert.assertEquals("{{{REDACTED-id}}}", replacement);

    }

    @Test
    public void replacement2() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final IdentifierFilterStrategy strategy = new IdentifierFilterStrategy();
        strategy.setRedactionFormat("{{{REDACTED-%n}}}");
        final String replacement = strategy.getReplacement("custom-name", "context", "documentId", "token", anonymizationService);

        Assert.assertEquals("{{{REDACTED-custom-name}}}", replacement);

    }

    @Test
    public void replacement3() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final IdentifierFilterStrategy strategy = new IdentifierFilterStrategy();
        strategy.setRedactionFormat("{{{REDACTED-%t-%n}}}");
        final String replacement = strategy.getReplacement("custom-name", "context", "documentId", "token", anonymizationService);

        Assert.assertEquals("{{{REDACTED-id-custom-name}}}", replacement);

    }

    @Test
    public void replacement4() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final IdentifierFilterStrategy strategy = new IdentifierFilterStrategy();
        strategy.setRedactionFormat("***%n-%t***");
        final String replacement = strategy.getReplacement("custom-name", "context", "documentId", "token", anonymizationService);

        Assert.assertEquals("***custom-name-id***", replacement);

    }

    @Test
    public void replacement5() throws IOException {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final IdentifierFilterStrategy strategy = new IdentifierFilterStrategy();
        strategy.setRedactionFormat("***%n-%n-%t***");
        final String replacement = strategy.getReplacement("custom-name", "context", "documentId", "token", anonymizationService);

        Assert.assertEquals("***custom-name-custom-name-id***", replacement);

    }

    @Test
    public void evaluateCondition1() throws IOException {

        IdentifierFilterStrategy strategy = new IdentifierFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "token startswith \"902\"", Collections.emptyMap());

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() throws IOException {

        IdentifierFilterStrategy strategy = new IdentifierFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "token == \"90210\"", Collections.emptyMap());

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() throws IOException {

        IdentifierFilterStrategy strategy = new IdentifierFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "12345", "token == \"90210\"", Collections.emptyMap());

        Assert.assertFalse(conditionSatisfied);

    }

}
