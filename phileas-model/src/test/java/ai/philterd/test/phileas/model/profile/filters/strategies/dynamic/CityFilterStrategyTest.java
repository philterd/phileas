package ai.philterd.test.phileas.model.profile.filters.strategies.dynamic;

import ai.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.dynamic.CityFilterStrategy;
import ai.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;

public class CityFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new CityFilterStrategy();
    }

}
