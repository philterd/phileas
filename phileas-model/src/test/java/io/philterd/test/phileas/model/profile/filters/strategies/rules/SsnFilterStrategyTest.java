package io.philterd.test.phileas.model.profile.filters.strategies.rules;

import io.philterd.phileas.model.objects.Replacement;
import io.philterd.phileas.model.profile.Crypto;
import io.philterd.phileas.model.profile.FPE;
import io.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import io.philterd.phileas.model.profile.filters.strategies.rules.SsnFilterStrategy;
import io.philterd.phileas.model.services.AnonymizationService;
import io.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SsnFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new SsnFilterStrategy();
    }

    @Test
    public void formatPreservingEncryption1() throws Exception {

        final FPE fpe = new FPE("2DE79D232DF5585D68CE47882AE256D6", "CBD09280979564");

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.FPE_ENCRYPT_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "123-45-6789", WINDOW, new Crypto(), fpe, anonymizationService, null);

        Assertions.assertEquals(11, replacement.getReplacement().length());
        Assertions.assertEquals("609-59-7486", replacement.getReplacement());

    }

    @Test
    public void formatPreservingEncryption2() throws Exception {

        final FPE fpe = new FPE("2DE79D232DF5585D68CE47882AE256D6", "CBD09280979564");

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.FPE_ENCRYPT_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "987 65 4321", WINDOW, new Crypto(), fpe, anonymizationService, null);

        Assertions.assertEquals(11, replacement.getReplacement().length());
        Assertions.assertEquals("195 55 5147", replacement.getReplacement());

    }

    @Test
    public void formatPreservingEncryption3() throws Exception {

        final FPE fpe = new FPE("2DE79D232DF5585D68CE47882AE256D6", "CBD09280979564");

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.FPE_ENCRYPT_REPLACE);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "987654321", WINDOW, new Crypto(), fpe, anonymizationService, null);

        Assertions.assertEquals(9, replacement.getReplacement().length());
        Assertions.assertEquals("195555147", replacement.getReplacement());

    }

    @Test
    public void lastFour1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = new SsnFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.LAST_4);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "4111111111111111", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("1111", replacement.getReplacement());

    }

    @Test
    public void lastFour2() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = new SsnFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.LAST_4);

        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "123-456-7890", WINDOW, new Crypto(), new FPE(), anonymizationService, null);

        Assertions.assertEquals("7890", replacement.getReplacement());

    }

}
