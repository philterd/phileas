/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.services.strategies.rules;

import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.services.anonymization.AbstractAnonymizationService;
import ai.philterd.phileas.services.anonymization.AnonymizationService;
import ai.philterd.phileas.services.anonymization.DateAnonymizationService;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class DateFilterStrategyTest extends AbstractFilterStrategyTest {

    @Override
    public DateFilterStrategy getFilterStrategy() {
        return new DateFilterStrategy();
    }

    public AbstractAnonymizationService getAnonymizationService() {
        return new DateAnonymizationService();
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

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "test@test.com", window, "token is \"birthdate\"", 1.0);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition2() {

        final String[] window = new String[]{"document", "filed", "10-05-2005", "on"};

        final AbstractFilterStrategy strategy = new DateFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "test@test.com", window, "token is \"birthdate\"", 1.0);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition3() {

        final String[] window = new String[]{"died", "on", "10-05-2005"};

        final AbstractFilterStrategy strategy = new DateFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "test@test.com", window, "token is \"deathdate\"", 1.0);

        Assertions.assertTrue(conditionSatisfied);

    }

    @Test
    public void evaluateCondition4() {

        final String[] window = new String[]{"died", "on", "10-05-2005"};

        final AbstractFilterStrategy strategy = new DateFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "test@test.com", window, "token is not \"deathdate\"", 1.0);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition5() {

        final String[] window = new String[]{"born", "on", "10-05-2005"};

        final AbstractFilterStrategy strategy = new DateFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "test@test.com", window, "token is not \"birthdate\"", 1.0);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition6() {

        final String[] window = new String[]{"born", "on", "10-05-2005"};

        final AbstractFilterStrategy strategy = new DateFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "test@test.com", window, "token is not \"birthdate or deathdate\"", 1.0);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition7() {

        final String[] window = new String[]{"died", "on", "10-05-2005"};

        final AbstractFilterStrategy strategy = new DateFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "test@test.com", window, "token is not \"birthdate or deathdate\"", 1.0);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void evaluateCondition8() {

        final String[] window = new String[]{"died", "on", "10-05-2005"};

        final AbstractFilterStrategy strategy = new DateFilterStrategy();

        final boolean conditionSatisfied = strategy.evaluateCondition(getPolicy(), "context", "test@test.com", window, "token == \"10-05-2005\"", 1.0);

        Assertions.assertFalse(conditionSatisfied);

    }

    @Test
    public void shiftReplacement1() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(2, 0, 0);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "2010-05-09", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        Assertions.assertEquals("2010-05-11", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement2() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(2, 2, 0);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "2010-05-09", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        Assertions.assertEquals("2010-07-11", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement3() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(-2, 2, 0);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "2010-05-09", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        Assertions.assertEquals("2010-07-07", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement4() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(0, 0, 0);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "2010-05-09", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        Assertions.assertEquals("2010-05-09", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement5() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(1, 1, 1);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("dd-MM-uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "05-09-2020", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        Assertions.assertEquals("06-10-2021", replacement.getReplacement());

    }

    @Test
    public void shiftReplacement6() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(1, 1, 1);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MMMM d uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "June 2 2021", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        Assertions.assertEquals("July 3 2022", replacement.getReplacement());

    }

    @Test
    public void shiftReplacementInvalidDate() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final AbstractFilterStrategy strategy = getShiftedFilterStrategy(1, 1, 1);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("dd-MM-uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "18-18-2020", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

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

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("dd-MM-uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "05-09-2020", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        Assertions.assertTrue(replacement.getReplacement().contains(" months ago"));

    }

    @Test
    public void relativeReplacement2() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("dd-MM-uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "05-09-2018", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        Assertions.assertTrue(replacement.getReplacement().contains(" months ago"));

    }

    @Test
    public void relativeReplacement3() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern(("MM-dd-yyyy")))
                .toFormatter();

        final LocalDateTime parsedDate = LocalDateTime.now().plusYears(5).plusMonths(4);
        final String date = parsedDate.format(formatter);

        final DateFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);
        strategy.setFutureDates(false);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("M-dd-uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  date, WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        // This is a future date and futures are disabled so expect redaction.
        Assertions.assertEquals("{{{REDACTED-date}}}", replacement.getReplacement());

    }

    @Test
    public void relativeReplacement4() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        // Minus days is to prevent test failures based on how late in the month we currently are.
        final LocalDateTime parsedDate = LocalDateTime.now().plusYears(5).plusMonths(3);
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("M-dd-uuuu");
        final String date = parsedDate.format(dtf);

        final DateFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);
        strategy.setFutureDates(true);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("M-dd-uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  date, WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        // This is a future date but futures are enabled.
        // The value will either be:
        // - "in 5 years 3 months"
        // - "in 5 years 2 months"
        // This is due to when in the month the test was run.
        final List<String> values = Arrays.asList("in 5 years 3 months", "in 5 years 2 months");
        Assertions.assertTrue(values.contains(replacement.getReplacement()));
    }

    @Test
    public void relativeReplacement5() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern(("MM-dd-yyyy")))
                .toFormatter();

        final LocalDateTime parsedDate = LocalDateTime.now().minusDays(3).minusMonths(7).minusYears(5);
        final String date = parsedDate.format(formatter);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MM-dd-uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  date, WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        Assertions.assertEquals("5 years 7 months ago", replacement.getReplacement());

    }

    @Test
    public void relativeReplacement6() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern("MMM yyyy"))
                .toFormatter();

        final LocalDateTime parsedDate = LocalDateTime.now().minusDays(3).minusMonths(7).minusYears(5);
        final String date = parsedDate.format(formatter);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MMM uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  date, WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        // Depending on when the test is run it may be either 7 or 8 months ago. That's fine.
        Assertions.assertTrue(
                replacement.getReplacement().equalsIgnoreCase("5 years 7 months ago") ||
                        replacement.getReplacement().equalsIgnoreCase("5 years 8 months ago"));

    }

    @Test
    public void relativeReplacement7() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern("MMM yyyy"))
                .toFormatter();

        final LocalDateTime parsedDate = LocalDateTime.now().minusDays(3).minusMonths(7);
        final String date = parsedDate.format(formatter);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RELATIVE);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MMM uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  date, WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        // Depending on when the test is run it may be either 7 or 8 months ago. That's fine.
        Assertions.assertTrue(
                replacement.getReplacement().equalsIgnoreCase("7 months ago") ||
                        replacement.getReplacement().equalsIgnoreCase("8 months ago"));


    }

    @Test
    public void truncateToYear1() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern(("MM-dd-yyyy")))
                .toFormatter();

        final LocalDateTime parsedDate = LocalDateTime.now();
        final String date = parsedDate.format(formatter);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.TRUNCATE_TO_YEAR);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MM-dd-uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  date, WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        Assertions.assertEquals(String.valueOf(parsedDate.getYear()), replacement.getReplacement());

    }

    @Test
    public void birthdate1() throws Exception {

        final String[] window = new String[]{"born", "on", "10-05-2005"};

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final AbstractFilterStrategy strategy = getFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.TRUNCATE_TO_YEAR);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MM-dd-uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  "10-05-2005", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

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

    @Test
    public void randomShift1() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();

        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern(("MM-dd-yyyy")))
                .toFormatter();

        final LocalDateTime parsedDate = LocalDateTime.now();
        final String date = parsedDate.format(formatter);

        final AbstractFilterStrategy strategy = getFilterStrategy();
        ((DateFilterStrategy)strategy).setShiftRandom(true);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);

        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MM-dd-uuuu").build();
        final Replacement replacement = strategy.getReplacement(contextService, "name", "context",  date, WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern);

        Assertions.assertNotEquals(parsedDate.getMonthValue() + "-" + parsedDate.getDayOfMonth() + "-" + parsedDate.getYear(), replacement.getReplacement());

    }

    @Test
    public void randomShiftWithContextScopeKeepsTheDaysBetweenDates() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd");

        final AbstractFilterStrategy strategy = getFilterStrategy();
        ((DateFilterStrategy)strategy).setShiftRandom(true);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);
        strategy.setReplacementScope(AbstractFilterStrategy.REPLACEMENT_SCOPE_CONTEXT);

        // January 30 and 31 would both become the end of February if a month were added to each.
        final String[] dates = {"2020-01-05", "2020-01-30", "2020-01-31", "2020-03-15", "2019-12-31"};

        // Each new context chooses its own random shift.
        for(int i = 0; i < 25; i++) {

            final ContextService contextService = new DefaultContextService();

            final LocalDate first = LocalDate.parse(dates[0], formatter);
            final LocalDate shiftedFirst = LocalDate.parse(strategy.getReplacement(contextService, "name", "context", dates[0], WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern).getReplacement(), formatter);
            Assertions.assertNotEquals(first, shiftedFirst);

            for(final String date : dates) {

                final LocalDate shifted = LocalDate.parse(strategy.getReplacement(contextService, "name", "context", date, WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern).getReplacement(), formatter);
                Assertions.assertEquals(ChronoUnit.DAYS.between(first, LocalDate.parse(date, formatter)), ChronoUnit.DAYS.between(shiftedFirst, shifted));

            }

        }

    }

    @Test
    public void randomShiftWithContextScopeIsKeptInTheContext() throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final ContextService contextService = new DefaultContextService();
        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{2}-\\d{2}-\\d{4}"), 0.75).withFormat("MM-dd-uuuu").build();

        final AbstractFilterStrategy strategy = getFilterStrategy();
        ((DateFilterStrategy)strategy).setShiftRandom(true);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);
        strategy.setReplacementScope(AbstractFilterStrategy.REPLACEMENT_SCOPE_CONTEXT);

        final String first = strategy.getReplacement(contextService, "name", "context", "05-09-2020", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern).getReplacement();
        final String second = strategy.getReplacement(contextService, "name", "context", "05-09-2020", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern).getReplacement();

        Assertions.assertEquals(first, second);
        Assertions.assertTrue(contextService.containsToken(DateFilterStrategy.CONTEXT_SHIFT_KEY));

    }

    @Test
    public void randomShiftWithContextScopeIsKeptAsAContextValue() throws Exception {

        // Keeps context values apart from token entries, as a ContextService that persists contexts would.
        final Map<String, String> values = new ConcurrentHashMap<>();
        final List<String> tokens = new CopyOnWriteArrayList<>();

        final ContextService contextService = new DefaultContextService() {

            @Override
            public String computeReplacementIfAbsent(final String token, final String filterType, final Supplier<String> replacementSupplier) {
                tokens.add(token);
                return super.computeReplacementIfAbsent(token, filterType, replacementSupplier);
            }

            @Override
            public String computeValueIfAbsent(final String key, final String filterType, final Supplier<String> valueSupplier) {
                return values.computeIfAbsent(key, k -> valueSupplier.get());
            }

        };

        final AnonymizationService anonymizationService = getAnonymizationService();
        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd");

        final AbstractFilterStrategy strategy = getFilterStrategy();
        ((DateFilterStrategy)strategy).setShiftRandom(true);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);
        strategy.setReplacementScope(AbstractFilterStrategy.REPLACEMENT_SCOPE_CONTEXT);

        final LocalDate first = LocalDate.parse(strategy.getReplacement(contextService, "name", "context", "2020-01-05", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern).getReplacement(), formatter);
        final LocalDate second = LocalDate.parse(strategy.getReplacement(contextService, "name", "context", "2020-03-15", WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern).getReplacement(), formatter);

        Assertions.assertEquals(String.valueOf(ChronoUnit.DAYS.between(LocalDate.of(2020, 1, 5), first)), values.get(DateFilterStrategy.CONTEXT_SHIFT_KEY));
        Assertions.assertEquals(ChronoUnit.DAYS.between(LocalDate.of(2020, 1, 5), LocalDate.of(2020, 3, 15)), ChronoUnit.DAYS.between(first, second));
        Assertions.assertFalse(tokens.contains(DateFilterStrategy.CONTEXT_SHIFT_KEY));
        Assertions.assertFalse(contextService.containsToken(DateFilterStrategy.CONTEXT_SHIFT_KEY));

    }

    @Test
    public void randomShiftMovesTheDateByAtLeastTheMinimum() throws Exception {
        assertRandomShiftsAtLeastTheMinimum(AbstractFilterStrategy.REPLACEMENT_SCOPE_DOCUMENT);
    }

    @Test
    public void randomShiftWithContextScopeMovesTheDateByAtLeastTheMinimum() throws Exception {
        assertRandomShiftsAtLeastTheMinimum(AbstractFilterStrategy.REPLACEMENT_SCOPE_CONTEXT);
    }

    // February 28, 2021 can come back unchanged from 29 days and 11 months forward and a year back, and about one draw
    // in 22 moves a date by fewer than 30 days, so 1,000 draws without the minimum would all but surely fail.
    private void assertRandomShiftsAtLeastTheMinimum(final String replacementScope) throws Exception {

        final AnonymizationService anonymizationService = getAnonymizationService();
        final FilterPattern filterPattern = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}"), 0.75).withFormat("uuuu-MM-dd").build();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd");

        final AbstractFilterStrategy strategy = getFilterStrategy();
        ((DateFilterStrategy)strategy).setShiftRandom(true);
        strategy.setStrategy(AbstractFilterStrategy.SHIFT);
        strategy.setReplacementScope(replacementScope);

        final LocalDate date = LocalDate.of(2021, 2, 28);

        for(int i = 0; i < 1000; i++) {

            final String replacement = strategy.getReplacement(new DefaultContextService(), "name", "context", date.format(formatter), WINDOW, new Crypto(), new FPE(), anonymizationService, filterPattern).getReplacement();
            final long days = ChronoUnit.DAYS.between(date, LocalDate.parse(replacement, formatter));
            Assertions.assertTrue(Math.abs(days) >= DateFilterStrategy.MIN_RANDOM_SHIFT_DAYS, "Shifted by " + days + " days.");

        }

    }

}