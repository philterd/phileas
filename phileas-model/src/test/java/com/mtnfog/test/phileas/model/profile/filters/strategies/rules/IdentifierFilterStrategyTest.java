package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.IdentifierFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.when;

public class IdentifierFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(IdentifierFilterStrategyTest.class);

    private AbstractFilterStrategy getFilterStrategy() {
        return new IdentifierFilterStrategy();
    }

    @Test
    public void replacement1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        final String replacement = strategy.getReplacement("custom-name", "context", "documentId", "token", new Crypto(), anonymizationService);

        Assert.assertEquals("{{{REDACTED-id}}}", replacement);

    }

    @Test
    public void replacement2() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setRedactionFormat("{{{REDACTED-%l}}}");
        final String replacement = strategy.getReplacement("custom-name", "context", "documentId", "token", new Crypto(), anonymizationService);

        Assert.assertEquals("{{{REDACTED-custom-name}}}", replacement);

    }

    @Test
    public void replacement3() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setRedactionFormat("{{{REDACTED-%t-%l}}}");
        final String replacement = strategy.getReplacement("custom-name", "context", "documentId", "token", new Crypto(), anonymizationService);

        Assert.assertEquals("{{{REDACTED-id-custom-name}}}", replacement);

    }

    @Test
    public void replacement4() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setRedactionFormat("***%l-%t***");
        final String replacement = strategy.getReplacement("custom-name", "context", "documentId", "token", new Crypto(), anonymizationService);

        Assert.assertEquals("***custom-name-id***", replacement);

    }

    @Test
    public void replacement5() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setRedactionFormat("***%l-%l-%t***");
        final String replacement = strategy.getReplacement("custom-name", "context", "documentId", "token", new Crypto(), anonymizationService);

        Assert.assertEquals("***custom-name-custom-name-id***", replacement);

    }

    @Test
    public void replacement6() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = new IdentifierFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("<ENTITY:%t>%v</ENTITY>");

        final String replacement = strategy.getReplacement("name", "context", "docId", "token", new Crypto(), anonymizationService);

        Assert.assertEquals("<ENTITY:id>token</ENTITY>", replacement);

    }

    @Test
    public void evaluateCondition1() throws IOException {

        IdentifierFilterStrategy strategy = new IdentifierFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "token startswith \"902\"", 1.0, "");

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() throws IOException {

        IdentifierFilterStrategy strategy = new IdentifierFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "token == \"90210\"", 1.0, "");

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() throws IOException {

        IdentifierFilterStrategy strategy = new IdentifierFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "12345", "token == \"90210\"", 1.0, "");

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "John Smith", "context == \"c1\"",  1.0, "");

        Assert.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", "context == \"ctx\"",  1.0, "");

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition6() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", "confidence > 0.5",  1.0, "");

        Assert.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition7() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", "confidence < 0.5",  1.0, "");

        Assert.assertFalse(conditionSatisfied);

    }

}
