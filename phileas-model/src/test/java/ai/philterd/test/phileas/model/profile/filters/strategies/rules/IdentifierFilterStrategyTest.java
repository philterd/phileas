package ai.philterd.test.phileas.model.profile.filters.strategies.rules;

import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.profile.Crypto;
import ai.philterd.phileas.model.profile.FPE;
import ai.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.IbanCodeFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.IdentifierFilterStrategy;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class IdentifierFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new IdentifierFilterStrategy();
    }

    @Test
    public void evaluateCondition1() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "90210", WINDOW, "classification == \"WV\"", 1.0, "WV");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void lastFour1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = new IdentifierFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.LAST_4);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "4111111111111111", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("1111", replacement.getReplacement());

    }

}