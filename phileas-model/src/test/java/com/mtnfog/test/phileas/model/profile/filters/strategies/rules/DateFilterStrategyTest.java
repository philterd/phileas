package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.DateFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.regex.Pattern;

public class DateFilterStrategyTest extends AbstractFilterStrategyTest {

    @Override
    public AbstractFilterStrategy getFilterStrategy() {
        return new DateFilterStrategy();
    }

    public DateFilterStrategy getShiftedFilterStrategy(int days, int months, int years) {

        final DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        dateFilterStrategy.setShiftDays(days);
        dateFilterStrategy.setShiftMonths(months);
        dateFilterStrategy.setShiftYears(years);

        return dateFilterStrategy;

    }

    @Test
    public void shiftReplacement1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(2, 0, 0);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "2010-05-09", new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("2010-05-11", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement2() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(2, 2, 0);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "2010-05-09", new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("2010-07-11", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement3() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(-2, 2, 0);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "2010-05-09", new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("2010-07-07", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement4() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(0, 0, 0);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "2010-05-09", new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("2010-05-09", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement5() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(1, 1, 1);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("dd-MM-uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "05-09-2020", new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("06-10-2021", replacement.getReplacement());

    }

    @Test
    public void shiftReplacementInvalidDate() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(1, 1, 1);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("dd-MM-uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "18-18-2020", new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("{{{REDACTED-date}}}", replacement.getReplacement());

    }

}