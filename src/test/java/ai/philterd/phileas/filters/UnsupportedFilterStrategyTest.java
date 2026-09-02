/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.services.filters.regex.SsnFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.DateFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.ZipCodeFilterStrategy;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An unrecognized strategy falls back to redaction, which fails closed, but the fallback must not be
 * silent: a typo in a reversible strategy would otherwise irreversibly redact. See issue #344.
 */
public class UnsupportedFilterStrategyTest extends AbstractFilterTest {

    // CRYPTO_REPLACE and FPE_ENCRYPT_REPLACE are rejected at construction without a key, so the
    // filters built here are given both; this is the key used by FilterConfigurationTest.
    private static final String VALID_AES_256_KEY = "9EE7A356FDFE43F069500B0086758346E66D8583E0CE1CFCA04E50F67ECCE5D1";

    /** Captures what Filter logs while a filter is built. */
    private static final class CapturingAppender extends AbstractAppender {

        private final List<String> messages = new CopyOnWriteArrayList<>();

        private CapturingAppender() {
            super("capture", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(final org.apache.logging.log4j.core.LogEvent event) {
            if (event.getLevel() == Level.WARN) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        }
    }

    /** Builds the filter with the given strategy and returns the WARN messages logged while doing so. */
    private List<String> warningsWhileBuilding(final AbstractFilterStrategy strategy) {

        // The appender goes on the root logger through the Configuration: adding it to the Filter
        // logger directly would create a LoggerConfig for that name that outlives removal, silencing
        // Filter's logging for every test that runs after this one in the same JVM.
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = context.getConfiguration();

        final CapturingAppender appender = new CapturingAppender();
        appender.start();
        configuration.addAppender(appender);
        configuration.getRootLogger().addAppender(appender, Level.WARN, null);
        context.updateLoggers();

        try {

            new SsnFilter(new FilterConfiguration.FilterConfigurationBuilder()
                    .withStrategies(List.of(strategy))
                    .withWindowSize(windowSize)
                    .withFPE(new FPE("2DE79D232DF5585D68CE47882AE256D6", "CBD09280979564"))
                    .withCrypto(new Crypto(VALID_AES_256_KEY, null))
                    .build());

            return appender.messages;

        } finally {
            configuration.getRootLogger().removeAppender(appender.getName());
            context.updateLoggers();
            appender.stop();
        }

    }

    private static SsnFilterStrategy ssnStrategy(final String strategy) {
        final SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();
        ssnFilterStrategy.setStrategy(strategy);
        return ssnFilterStrategy;
    }

    @Test
    public void unknownStrategyIsLoggedAtWarn() {

        final List<String> warnings = warningsWhileBuilding(ssnStrategy("MSK"));

        Assertions.assertEquals(1, warnings.size(), warnings.toString());
        final String warning = warnings.get(0);

        // The value and the filter both have to appear, or the warning cannot be acted on.
        Assertions.assertTrue(warning.contains("\"MSK\""), warning);
        Assertions.assertTrue(warning.contains("ssn"), warning);
        Assertions.assertTrue(warning.contains("REDACT"), warning);

    }

    @Test
    public void emptyAndBlankStrategiesAreLoggedAtWarn() {

        for (final String strategy : List.of("", "   ", "TOTAL_NONSENSE")) {
            Assertions.assertEquals(1, warningsWhileBuilding(ssnStrategy(strategy)).size(),
                    "expected a warning for strategy '" + strategy + "'");
        }

    }

    @Test
    public void aStrategyValidOnAnotherFilterIsLoggedAtWarn() {

        // ZERO_LEADING is implemented by the zip code filter only.
        final List<String> warnings = warningsWhileBuilding(ssnStrategy("ZERO_LEADING"));

        Assertions.assertEquals(1, warnings.size(), warnings.toString());
        Assertions.assertTrue(warnings.get(0).contains("ZERO_LEADING"), warnings.get(0));

    }

    @Test
    public void supportedStrategiesAreNotLogged() {

        for (final String strategy : new SsnFilterStrategy().getAcceptedStrategies()) {
            Assertions.assertTrue(warningsWhileBuilding(ssnStrategy(strategy)).isEmpty(),
                    "did not expect a warning for strategy '" + strategy + "'");
        }

        // Case is not significant anywhere else, so it must not be here either.
        Assertions.assertTrue(warningsWhileBuilding(ssnStrategy("crypto_replace")).isEmpty());

        // A policy that omits the strategy keeps the REDACT default and is not a typo.
        Assertions.assertTrue(warningsWhileBuilding(new SsnFilterStrategy()).isEmpty());

    }

    @Test
    public void theFallbackToRedactionIsUnchanged() throws Exception {

        final SsnFilter filter = new SsnFilter(new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(ssnStrategy("MSK")))
                .withWindowSize(windowSize)
                .build());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123-45-6789.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 22, FilterType.SSN));
        Assertions.assertEquals("{{{REDACTED-ssn}}}", filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void eachStrategyDeclaresWhatItImplements() throws Exception {

        // The date and zip code filters accept names the standard set does not, and do not accept
        // some that it does; guard those so the declarations cannot drift from the dispatch chains.
        Assertions.assertTrue(new DateFilterStrategy().getAcceptedStrategies().contains(AbstractFilterStrategy.SHIFT));
        Assertions.assertFalse(new DateFilterStrategy().getAcceptedStrategies().contains(AbstractFilterStrategy.LAST_4));
        Assertions.assertFalse(new SsnFilterStrategy().getAcceptedStrategies().contains(AbstractFilterStrategy.SHIFT));

        // ZipCodeFilterStrategy declares TRUNCATE and ZERO_LEADING in lower case, and comparison is
        // case-insensitive everywhere else, so a zip policy using either must not be warned about.
        final Set<String> zipAccepted = new HashSet<>();
        for (final String strategy : new ZipCodeFilterStrategy().getAcceptedStrategies()) {
            zipAccepted.add(strategy.toUpperCase(Locale.ROOT));
        }
        Assertions.assertTrue(zipAccepted.contains("ZERO_LEADING"), zipAccepted.toString());
        Assertions.assertTrue(zipAccepted.contains("TRUNCATE"), zipAccepted.toString());

    }

}
