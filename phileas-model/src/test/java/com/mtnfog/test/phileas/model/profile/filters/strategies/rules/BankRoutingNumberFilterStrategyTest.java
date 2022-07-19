package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FPE;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.BankRoutingNumberFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.CreditCardFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class BankRoutingNumberFilterStrategyTest extends AbstractFilterStrategyTest {

    @Override
    public AbstractFilterStrategy getFilterStrategy() {
        return new BankRoutingNumberFilterStrategy();
    }

    @Test
    public void formatPreservingEncryption1() throws Exception {

        final FPE fpe = new FPE("2DE79D232DF5585D68CE47882AE256D6", "CBD09280979564");

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.FPE_ENCRYPT_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "091000022", WINDOW, new Crypto(), fpe, anonymizationService, null);

        Assertions.assertEquals(9, replacement.getReplacement().length());
        Assertions.assertEquals("970881062", replacement.getReplacement());

    }

}
