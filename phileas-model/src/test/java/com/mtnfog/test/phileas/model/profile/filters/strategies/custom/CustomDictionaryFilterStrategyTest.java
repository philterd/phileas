package com.mtnfog.test.phileas.model.profile.filters.strategies.custom;

import com.mtnfog.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

public class CustomDictionaryFilterStrategyTest {

    private static final Logger LOGGER = LogManager.getLogger(CustomDictionaryFilterStrategyTest.class);

    @Test
    public void evaluateCondition1() throws IOException {

        CustomDictionaryFilterStrategy strategy = new CustomDictionaryFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentId", "90210", "token startswith \"902\"", Collections.emptyMap());

        Assert.assertTrue(conditionSatisfied);

    }

}
