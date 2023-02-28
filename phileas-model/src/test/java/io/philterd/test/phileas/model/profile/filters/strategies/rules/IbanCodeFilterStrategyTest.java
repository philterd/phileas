package io.philterd.test.phileas.model.profile.filters.strategies.rules;

import io.philterd.phileas.model.objects.Replacement;
import io.philterd.phileas.model.profile.Crypto;
import io.philterd.phileas.model.profile.FPE;
import io.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import io.philterd.phileas.model.profile.filters.strategies.rules.CreditCardFilterStrategy;
import io.philterd.phileas.model.profile.filters.strategies.rules.IbanCodeFilterStrategy;
import io.philterd.phileas.model.services.AnonymizationService;
import io.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class IbanCodeFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new IbanCodeFilterStrategy();
    }

    @Test
    public void lastFour1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = new IbanCodeFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.LAST_4);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "4111111111111111", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("1111", replacement.getReplacement());

    }

}
