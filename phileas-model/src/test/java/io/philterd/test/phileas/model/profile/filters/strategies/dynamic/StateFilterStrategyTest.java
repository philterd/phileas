package io.philterd.test.phileas.model.profile.filters.strategies.dynamic;

import io.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import io.philterd.phileas.model.profile.filters.strategies.dynamic.StateFilterStrategy;
import io.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;

public class StateFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new StateFilterStrategy();
    }

}
