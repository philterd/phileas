package io.philterd.test.phileas.model.profile.filters.strategies.ai;

import io.philterd.phileas.model.objects.Replacement;
import io.philterd.phileas.model.profile.Crypto;
import io.philterd.phileas.model.profile.FPE;
import io.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import io.philterd.phileas.model.profile.filters.strategies.ai.PersonsFilterStrategy;
import io.philterd.phileas.model.services.AnonymizationCacheService;
import io.philterd.phileas.model.services.AnonymizationService;
import io.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

public class PersonsFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new PersonsFilterStrategy();
    }

    @Test
    public void replacement1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        strategy.setStaticReplacement("static-value");

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("static-value", replacement.getReplacement());

    }

    @Test
    public void replacement2() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("REDACTION-%t");

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("REDACTION-person", replacement.getReplacement());

    }

    @Test
    public void replacement3() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationCacheService.get("context", "token")).thenReturn("random");
        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

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

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("{{{REDACTED-person}}}", replacement.getReplacement());

    }

    @Test
    public void replacement5() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = new PersonsFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.REDACT);
        strategy.setRedactionFormat("<ENTITY:%t>%v</ENTITY>");

        final Replacement replacement = strategy.getReplacement("PER", "context", "docId", "token", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("<ENTITY:person>token</ENTITY>", replacement.getReplacement());

    }

    @Test
    public void replacement6() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);
        final AnonymizationCacheService anonymizationCacheService = Mockito.mock(AnonymizationCacheService.class);

        when(anonymizationService.getAnonymizationCacheService()).thenReturn(anonymizationCacheService);

        final AbstractFilterStrategy strategy = new PersonsFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.ABBREVIATE);

        Replacement replacement;

        replacement = strategy.getReplacement("PER", "context", "docId", "John Smith", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("JS", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "John P. Smith", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("JPS", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "John P Smith", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("JPS", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "John P.", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("JP", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "John", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("J", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "J Smith", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("JS", replacement.getReplacement());

        replacement = strategy.getReplacement("PER", "context", "docId", "J. Peter Smith", WINDOW, new Crypto(), new FPE(), anonymizationService, null);
        Assertions.assertEquals("JPS", replacement.getReplacement());

    }

    @Test
    public void evaluateCondition1() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW,"confidence > 0.25",  0.5, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW,"type == PER",  1.0, "PER");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW,"type == PER",  1.0, "LOC");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW,"confidence == 0.5",  0.5, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW,"confidence != 0.5",  0.6, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition6() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW,"confidence != 0.5",  0.5, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition7() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW,"confidence != 0.5 and type == PER",  0.5, "LOC");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition8() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW,"confidence != 0.5 and type != LOC",  0.5, "PER");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition9() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW,"confidence > 0.4 and type == PER",  0.5, "PER");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition10() {

        final AbstractFilterStrategy strategy = getFilterStrategy();


        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW,"confidence < 0.4 and type == PER",  0.5, "PER");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition11() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "John Smith", WINDOW,"context == \"c1\"",  1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition12() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", WINDOW,"context == \"ctx\"",  1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition13() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", WINDOW,"token == \"John Smith\"",  1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition14() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("ctx", "documentId", "John Smith", WINDOW,"token != \"John Smith\"",  1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

}
