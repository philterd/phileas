package io.philterd.test.phileas.model.profile.filters.strategies.rules;

import io.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import io.philterd.phileas.model.profile.filters.strategies.rules.IpAddressFilterStrategy;
import io.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;

public class IpAddressFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new IpAddressFilterStrategy();
    }

}
