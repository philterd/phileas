
package ai.philterd.phileas.services.strategies;

import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.services.anonymization.AgeAnonymizationService;
import ai.philterd.phileas.services.anonymization.AnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.strategies.rules.AgeFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

public class StrategySpecificAnonymizationTest {

    @Test
    public void testStrategySpecificAnonymization() throws Exception {

        final AnonymizationService defaultAnonymizationService = new AgeAnonymizationService(new DefaultContextService(), new SecureRandom());

        // Create a strategy with specific candidates
        final AbstractFilterStrategy strategy = new AgeFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);
        strategy.setAnonymizationCandidates(Arrays.asList("99", "100"));
        
        // Manual initialization normally done by Filter constructor
        final AnonymizationService strategyAnonymizationService = new AgeAnonymizationService(new DefaultContextService(), new SecureRandom(), strategy.getAnonymizationCandidates());
        strategy.setAnonymizationService(strategyAnonymizationService);

        final Replacement replacement = strategy.getReplacement("age", "context", "25", new String[]{"25"}, new Crypto(), new FPE(), defaultAnonymizationService, null);

        // The replacement should be from our candidates list, not from the default service
        Assertions.assertTrue(replacement.getReplacement().equals("99") || replacement.getReplacement().equals("100"));

    }

    @Test
    public void testDefaultAnonymization() throws Exception {

        // Default candidates for AgeAnonymizationService are usually random ages.
        // We'll provide a fixed list to the default service to distinguish it.
        final AnonymizationService defaultAnonymizationService = new AgeAnonymizationService(new DefaultContextService(), new SecureRandom(), Collections.singletonList("default-age"));

        final AbstractFilterStrategy strategy = new AgeFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);
        // No strategy-specific service set

        final Replacement replacement = strategy.getReplacement("age", "context", "25", new String[]{"25"}, new Crypto(), new FPE(), defaultAnonymizationService, null);

        // Should use the default service
        Assertions.assertEquals("default-age", replacement.getReplacement());

    }
}
