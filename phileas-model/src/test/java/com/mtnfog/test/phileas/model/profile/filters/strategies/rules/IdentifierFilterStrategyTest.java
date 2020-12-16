package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.IbanCodeFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.IdentifierFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
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

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "90210", "classification == \"WV\"", 1.0, "WV");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void lastFour1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = new IdentifierFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.LAST_4);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "4111111111111111", new Crypto(), anonymizationService, null);

        Assertions.assertEquals("1111", replacement.getReplacement());

    }

}
