package io.philterd.test.phileas.model.profile.filters.strategies.dynamic;

import io.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import io.philterd.phileas.model.profile.filters.strategies.dynamic.SurnameFilterStrategy;
import io.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;

public class SurnameFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new SurnameFilterStrategy();
    }

}
