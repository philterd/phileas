package com.mtnfog.test.phileas.model.profile.filters.strategies.dynamic;

import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.CityFilterStrategy;
import com.mtnfog.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CityFilterStrategyTest extends AbstractFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(CityFilterStrategyTest.class);

    public AbstractFilterStrategy getFilterStrategy() {
        return new CityFilterStrategy();
    }

}
