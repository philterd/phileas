package io.philterd.test.phileas.model.profile.filters.strategies.dynamic;

import io.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import io.philterd.phileas.model.profile.filters.strategies.dynamic.HospitalAbbreviationFilterStrategy;
import io.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;

public class HospitalAbbreviationFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new HospitalAbbreviationFilterStrategy();
    }

}
