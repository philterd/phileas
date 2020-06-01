package com.mtnfog.test.phileas.model.profile.filters.strategies.dynamic;

import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.HospitalFilterStrategy;
import com.mtnfog.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HospitalFilterStrategyTest extends AbstractFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(HospitalFilterStrategyTest.class);

    public AbstractFilterStrategy getFilterStrategy() {
        return new HospitalFilterStrategy();
    }

}
