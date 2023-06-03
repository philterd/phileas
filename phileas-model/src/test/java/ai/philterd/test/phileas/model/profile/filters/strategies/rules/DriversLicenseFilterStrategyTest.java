package ai.philterd.test.phileas.model.profile.filters.strategies.rules;

import ai.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.DriversLicenseFilterStrategy;
import ai.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DriversLicenseFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new DriversLicenseFilterStrategy();
    }

    @Test
    public void evaluateCondition1() {

        final AbstractFilterStrategy strategy = getFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "90210", WINDOW, "classification == \"WV\"", 1.0, "WV");

        Assertions.assertTrue(conditionSatisfied);

    }

}
