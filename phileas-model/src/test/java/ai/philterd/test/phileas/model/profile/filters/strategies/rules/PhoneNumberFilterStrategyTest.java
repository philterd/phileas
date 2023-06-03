package ai.philterd.test.phileas.model.profile.filters.strategies.rules;

import ai.philterd.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.profile.filters.strategies.rules.PhoneNumberFilterStrategy;
import ai.philterd.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;

public class PhoneNumberFilterStrategyTest extends AbstractFilterStrategyTest {

    public AbstractFilterStrategy getFilterStrategy() {
        return new PhoneNumberFilterStrategy();
    }

}
