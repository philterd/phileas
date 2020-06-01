package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.AgeFilterStrategy;
import com.mtnfog.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AgeFilterStrategyTest extends AbstractFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(AgeFilterStrategyTest.class);

    @Override
    public AbstractFilterStrategy getFilterStrategy() {
        return new AgeFilterStrategy();
    }

}
