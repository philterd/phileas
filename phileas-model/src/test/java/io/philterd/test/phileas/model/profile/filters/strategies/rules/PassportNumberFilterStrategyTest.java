package io.philterd.test.phileas.model.profile.filters.strategies.rules;

import io.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import io.philterd.phileas.model.profile.filters.strategies.rules.PassportNumberFilterStrategy;
import io.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class PassportNumberFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new PassportNumberFilterStrategy();
    }

    @Test
    public void evaluateCondition1() throws IOException {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "986001231", WINDOW, "classification == \"US\"", 1.0, "US");

        Assertions.assertTrue(conditionSatisfied);

    }

}
