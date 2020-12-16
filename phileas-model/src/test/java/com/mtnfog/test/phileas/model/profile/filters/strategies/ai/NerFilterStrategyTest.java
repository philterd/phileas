package com.mtnfog.test.phileas.model.profile.filters.strategies.ai;

import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.ai.NerFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

public class NerFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new NerFilterStrategy();
    }

    @Test
    public void replacement1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("static-value");

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", new Crypto(), anonymizationService, null);

        Assertions.assertEquals("static-value", replacement.getReplacement());

    }

    @Test
    public void replacement2() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("REDACTION-%t");

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", new Crypto(), anonymizationService, null);

        Assertions.assertEquals("REDACTION-entity", replacement.getReplacement());

    }

    @Test
    public void replacement3() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationCacheService.get("context", "token")).thenReturn("random");
        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", new Crypto(), anonymizationService, null);

        Assertions.assertNotEquals("random", replacement.getReplacement());

    }

    @Test
    public void replacement4() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationCacheService.get("context", "token")).thenReturn("random");
        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy("something-wrong");

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", new Crypto(), anonymizationService, null);

        Assertions.assertEquals("{{{REDACTED-entity}}}", replacement.getReplacement());

    }

    @Test
    public void replacement5() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = new NerFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("<ENTITY:%t>%v</ENTITY>");

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", new Crypto(), anonymizationService, null);

        Assertions.assertEquals("<ENTITY:entity>token</ENTITY>", replacement.getReplacement());

    }

    @Test
    public void replacement6() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = new NerFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.ABBREVIATE);

        Replacement replacement;

        replacement = strategy.getReplacement("PER", "context", "docId", "John Smith", new Crypto(), anonymizationService, null);
        Assertions.assertEquals("JS", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "John P. Smith", new Crypto(), anonymizationService, null);
        Assertions.assertEquals("JPS", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "John P Smith", new Crypto(), anonymizationService, null);
        Assertions.assertEquals("JPS", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "John P.", new Crypto(), anonymizationService, null);
        Assertions.assertEquals("JP", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "John", new Crypto(), anonymizationService, null);
        Assertions.assertEquals("J", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "J Smith", new Crypto(), anonymizationService, null);
        Assertions.assertEquals("JS", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "J. Peter Smith", new Crypto(), anonymizationService, null);
        Assertions.assertEquals("JPS", replacement.getReplacement());

    }

    @Test
    public void evaluateCondition1() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", "confidence > 0.25",  0.5, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", "type == PER",  1.0, "PER");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", "type == PER",  1.0, "LOC");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", "confidence == 0.5",  0.5, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", "confidence != 0.5",  0.6, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition6() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", "confidence != 0.5",  0.5, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition7() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", "confidence != 0.5 and type == PER",  0.5, "LOC");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition8() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", "confidence != 0.5 and type != LOC",  0.5, "PER");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition9() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", "confidence > 0.4 and type == PER",  0.5, "PER");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition10() {

        final AbstractFilterStrategy strategy = getFilterStrategy();


        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", "confidence < 0.4 and type == PER",  0.5, "PER");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition11() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", "context == \"c1\"",  1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition12() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", "context == \"ctx\"",  1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition13() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", "token == \"John Smith\"",  1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition14() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", "token != \"John Smith\"",  1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

}
