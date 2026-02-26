package ai.philterd.phileas.filters;

import ai.philterd.phileas.filters.rules.dictionary.FuzzyDictionaryFilter;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.SensitivityLevel;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.regex.SsnFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.dynamic.FirstNameFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ContextInsertionTest extends AbstractFilterTest {

    @Test
    public void testContextInsertion() throws Exception {
        DefaultContextService contextService = new DefaultContextService();

        // 1. SSN Filter
        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();
        ssnFilterStrategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);
        ssnFilterStrategy.setReplacementScope(AbstractFilterStrategy.REPLACEMENT_SCOPE_CONTEXT);

        FilterConfiguration ssnConfig = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(ssnFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .build();
        SsnFilter ssnFilter = new SsnFilter(ssnConfig);

        // 2. FirstName Filter (using FuzzyDictionaryFilter)
        FirstNameFilterStrategy firstNameFilterStrategy = new FirstNameFilterStrategy();
        firstNameFilterStrategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);
        firstNameFilterStrategy.setReplacementScope(AbstractFilterStrategy.REPLACEMENT_SCOPE_CONTEXT);

        FilterConfiguration firstNameConfig = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(firstNameFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .build();
        FuzzyDictionaryFilter firstNameFilter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, firstNameConfig, SensitivityLevel.LOW, true);

        // Filter SSN
        ssnFilter.filter(getPolicy(), "ctx", PIECE, "The ssn is 123-45-6789.");
        
        // Filter FirstName
        firstNameFilter.filter(getPolicy(), "ctx", PIECE, "His name is Melissa.");

        // Check ContextService
        Assertions.assertTrue(contextService.containsToken("123-45-6789"), "SSN should be in context");
        // Assertions.assertTrue(contextService.containsToken("ctx", "ctx", "Melissa"), "First name should be in context");

    }

}
