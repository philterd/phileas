package ai.philterd.test.phileas.model.profile.filters.strategies.rules;

import ai.philterd.phileas.model.objects.Replacement;
import ai.philterd.phileas.model.profile.Crypto;
import ai.philterd.phileas.model.profile.FPE;
import ai.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.AgeFilterStrategy;
import ai.philterd.phileas.model.services.AnonymizationService;
import ai.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AgeFilterStrategyTest extends AbstractFilterStrategyTest {

    @Override
    public AbstractFilterStrategy getFilterStrategy() {
        return new AgeFilterStrategy();
    }

}
