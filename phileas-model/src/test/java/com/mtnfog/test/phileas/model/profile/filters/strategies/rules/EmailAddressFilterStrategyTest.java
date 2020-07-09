package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.EmailAddressFilterStrategy;
import com.mtnfog.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EmailAddressFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new EmailAddressFilterStrategy();
    }

    @Test
    public void evaluateCondition1() {

        final EmailAddressFilterStrategy strategy = new EmailAddressFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid",  "test@test.com", "token == \"test@test.com\"", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

}
