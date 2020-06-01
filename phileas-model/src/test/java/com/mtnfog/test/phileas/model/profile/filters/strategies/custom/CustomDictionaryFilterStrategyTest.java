package com.mtnfog.test.phileas.model.profile.filters.strategies.custom;

import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy;
import com.mtnfog.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomDictionaryFilterStrategyTest extends AbstractFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(CustomDictionaryFilterStrategyTest.class);

    public AbstractFilterStrategy getFilterStrategy() {
        return new CustomDictionaryFilterStrategy();
    }

}
