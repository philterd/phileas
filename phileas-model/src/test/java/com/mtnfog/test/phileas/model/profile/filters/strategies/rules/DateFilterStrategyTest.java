package com.mtnfog.test.phileas.model.profile.filters.strategies.rules;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Replacement;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.DateFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.EmailAddressFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import com.mtnfog.test.phileas.model.profile.filters.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class DateFilterStrategyTest extends AbstractFilterStrategyTest {

    @Override
    public DateFilterStrategy getFilterStrategy() {
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
    public void evaluateCondition1() {

        final String[] window = new String[]{"born", "on", "10-05-2005"};

        final AbstractFilterStrategy strategy = new DateFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "test@test.com", window, "token is \"birthdate\"", 1.0, "");

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() {

        final String[] window = new String[]{"document", "filed", "10-05-2005", "on"};

        final AbstractFilterStrategy strategy = new DateFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition("context", "documentid", "test@test.com", window, "token is \"birthdate\"", 1.0, "");

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void shiftReplacement1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(2, 0, 0);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "2010-05-09", WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("2010-05-11", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement2() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(2, 2, 0);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "2010-05-09", WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("2010-07-11", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement3() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(-2, 2, 0);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "2010-05-09", WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("2010-07-07", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement4() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(0, 0, 0);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "2010-05-09", WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("2010-05-09", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement5() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(1, 1, 1);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("dd-MM-uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "05-09-2020", WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("06-10-2021", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement6() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(1, 1, 1);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MMMM d uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "June 2 2021", WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("July 3 2022", replacement.getReplacement());

    }

    @Test
    public void shiftReplacementInvalidDate() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(1, 1, 1);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("dd-MM-uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "18-18-2020", WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("{{{REDACTED-date}}}", replacement.getReplacement());

    }

    @Test
    public void getReadableDate1() {

        final LocalDateTime parsedDate = LocalDateTime.now().minusDays(3).minusMonths(2);
        final LocalDateTime currentDate = LocalDateTime.now();

        final DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        final String replacement = dateFilterStrategy.getReadableDate(parsedDate, currentDate, "token", "date", FilterType.DATE);

        Assertions.assertEquals("2 months ago", replacement);

    }

    @Test
    public void getReadableDate2() {

        final LocalDateTime parsedDate = LocalDateTime.now().minusDays(3).minusMonths(2).minusYears(10);
        final LocalDateTime currentDate = LocalDateTime.now();

        final DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        final String replacement = dateFilterStrategy.getReadableDate(parsedDate, currentDate, "token", "date", FilterType.DATE);

        Assertions.assertEquals("10 years 2 months ago", replacement);

    }

    @Test
    public void relativeReplacement1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("dd-MM-uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "05-09-2020", WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertTrue(replacement.getReplacement().contains(" months ago"));

    }

    @Test
    public void relativeReplacement2() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("dd-MM-uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "05-09-2018", WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertTrue(replacement.getReplacement().contains(" months ago"));

    }

    @Test
    public void relativeReplacement3() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern(("MM-dd-yyyy")))
                .toFormatter();

        final LocalDateTime parsedDate = LocalDateTime.now().plusYears(5).plusMonths(4);
        final String date = parsedDate.format(formatter);

        final DateFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);
        strategy.setFutureDates(false);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("M-dd-uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", date, WINDOW, new Crypto(), anonymizationService, filterPattern);

        // This is a future date and futures are disabled so expect redaction.
        Assertions.assertEquals("{{{REDACTED-date}}}", replacement.getReplacement());

    }

    @Test
    public void relativeReplacement4() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        // Minus days is to prevent test failures based on how late in the month we currently are.
        final LocalDateTime parsedDate = LocalDateTime.now().plusYears(5).plusMonths(3);
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("M-dd-uuuu");
        final String date = parsedDate.format(dtf);

        final DateFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);
        strategy.setFutureDates(true);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("M-dd-uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", date, WINDOW, new Crypto(), anonymizationService, filterPattern);

        // This is a future date but futures are enabled.
        Assertions.assertEquals("in 5 years 3 months", replacement.getReplacement());

    }

    @Test
    public void relativeReplacement5() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern(("MM-dd-yyyy")))
                .toFormatter();

        final LocalDateTime parsedDate = LocalDateTime.now().minusDays(3).minusMonths(7).minusYears(5);
        final String date = parsedDate.format(formatter);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MM-dd-uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", date, WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("5 years 7 months ago", replacement.getReplacement());

    }

    @Test
    public void relativeReplacement6() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern("MMM yyyy"))
                .toFormatter();

        final LocalDateTime parsedDate = LocalDateTime.now().minusDays(3).minusMonths(7).minusYears(5);
        final String date = parsedDate.format(formatter);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MMM uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", date, WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("5 years 8 months ago", replacement.getReplacement());

    }

    @Test
    public void relativeReplacement7() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern("MMM yyyy"))
                .toFormatter();

        final LocalDateTime parsedDate = LocalDateTime.now().minusDays(3).minusMonths(7);
        final String date = parsedDate.format(formatter);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MMM uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", date, WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("8 months ago", replacement.getReplacement());

    }

    @Test
    public void truncateToYear1() throws Exception {

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern(("MM-dd-yyyy")))
                .toFormatter();

        final LocalDateTime parsedDate = LocalDateTime.now();
        final String date = parsedDate.format(formatter);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.TRUNCATE_TO_YEAR);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MM-dd-uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", date, WINDOW, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals(String.valueOf(parsedDate.getYear()), replacement.getReplacement());

    }

    @Test
    public void birthdate1() throws Exception {

        final String[] window = new String[]{"born", "on", "10-05-2005"};

        final AnonymizationService anonymizationService = Mockito.mock(AnonymizationService.class);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.TRUNCATE_TO_YEAR);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MM-dd-uuuu").build();
        final Replacement replacement = strategy.getReplacement("name", "context", "docId", "10-05-2005", window, new Crypto(), anonymizationService, filterPattern);

        Assertions.assertEquals("2005", replacement.getReplacement());

    }

    @Test
    public void format1() {

        // TODO: Change this to execute the code in DateFilterStrategy under RELATIVE instead of here.

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{1,2}" + "-" + "\\d{2,4}"), 75).withFormat("M-u".replaceAll("-", "-")).build();
        final String token = "09-2021";

        DateTimeFormatter dtf = new DateTimeFormatterBuilder()
                .appendPattern(filterPattern.getFormat())
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .toFormatter();

        // final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(filterPattern.getFormat(), Locale.US).withResolverStyle(ResolverStyle.STRICT);
        final LocalDateTime parsedDate = LocalDate.parse(token, dtf).atStartOfDay();

        LOGGER.info(parsedDate.toString());

    }

}