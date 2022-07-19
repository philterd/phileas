package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FPE;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.CreditCardFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.regex.Pattern;

public class CreditCardFilterStrategyTest extends AbstractFilterStrategyTest {

    @Override
    public AbstractFilterStrategy getFilterStrategy() {
        return new CreditCardFilterStrategy();
    }

    @Test
    public void formatPreservingEncryption1() throws Exception {

        final FPE fpe = new FPE("2DE79D232DF5585D68CE47882AE256D6", "CBD09280979564");

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.FPE_ENCRYPT_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "4111111111111111", WINDOW, new Crypto(), fpe, anonymizationService, null);

        Assertions.assertEquals(16, replacement.getReplacement().length());
        Assertions.assertEquals("3342630431069827", replacement.getReplacement());

        // The encrypted credit card number must be a valid credit card number.
        final LuhnCheckDigit luhnCheckDigit = new LuhnCheckDigit();
        luhnCheckDigit.isValid(replacement.getReplacement());

    }

    @Test
    public void lastFour1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = new CreditCardFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.LAST_4);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "4111111111111111", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("1111", replacement.getReplacement());

    }

}
