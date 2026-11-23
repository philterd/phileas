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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.utils.Encryption;
import ai.philterd.phileas.utils.FormatPreservingEncryptionException;
import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

/**
 * A span trimmed where it overlaps a span of another filter is replaced by what its filter makes of the
 * text it reports, not of everything the filter matched.
 */
class TrimmedSpanReplacementTest {

    private static final String INPUT = "DOB 1/2/20 CAD stable.";

    private static final String CRYPTO_KEY = "9EE7A356FDFE43F069500B0086758346E66D8583E0CE1CFCA04E50F67ECCE5D1";
    private static final String CRYPTO_IV = "B674D3B8F1C025AEFF8F6D5FA1AEAD3A";
    private static final String FPE_KEY = "EF4359D8D580AA4F7F036D6F04FC6A94";
    private static final String FPE_TWEAK = "D8E7920AFA330A73";

    private static String policy(final String strategy, final String extra) {
        return policyWithStrategy("{\"strategy\": \"" + strategy + "\"}", extra);
    }

    private static String policyWithStrategy(final String strategy, final String extra) {
        final String strategies = "[" + strategy + "]";
        return "{" + extra + "\"crypto\": {\"key\": \"" + CRYPTO_KEY + "\", \"iv\": \"" + CRYPTO_IV + "\"}, "
                + "\"fpe\": {\"key\": \"" + FPE_KEY + "\", \"tweak\": \"" + FPE_TWEAK + "\"}, "
                + "\"identifiers\": {\"date\": {\"dateFilterStrategies\": " + strategies + "}, "
                + "\"currency\": {\"currencyFilterStrategies\": " + strategies + "}}}";
    }

    private static TextFilterResult filter(final String policy, final ContextService contextService,
                                           final String context, final String input) throws Exception {

        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), contextService, new InMemoryVectorService(), null);

        return service.filter(new Gson().fromJson(policy, Policy.class), context, input);

    }

    private static TextFilterResult filter(final String policy, final String input) throws Exception {
        return filter(policy, new DefaultContextService(), "context", input);
    }

    private static Span appliedSpan(final TextFilterResult result, final FilterType filterType) {
        return result.getExplanation().appliedSpans().stream()
                .filter(span -> span.getFilterType() == filterType)
                .findFirst().orElseThrow();
    }

    // Puts each span's original back in place of its replacement.
    private static String restore(final String input, final List<Span> spans, final java.util.function.Function<String, String> reverse) {

        final StringBuilder sb = new StringBuilder();
        int copiedTo = 0;

        for (final Span span : spans.stream().sorted(java.util.Comparator.comparingInt(Span::getCharacterStart)).toList()) {
            sb.append(input, copiedTo, span.getCharacterStart()).append(reverse.apply(span.getReplacement()));
            copiedTo = span.getCharacterEnd();
        }

        return sb.append(input.substring(copiedTo)).toString();

    }

    private static String decrypt(final String replacement) {
        try {
            return Encryption.decrypt(replacement.substring(2, replacement.length() - 2), new Crypto(CRYPTO_KEY, CRYPTO_IV));
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void cryptoReplaceRestoresTheOriginalText() throws Exception {

        final TextFilterResult result = filter(policy("CRYPTO_REPLACE", ""), INPUT);

        final Span currency = appliedSpan(result, FilterType.CURRENCY);
        Assertions.assertEquals("CAD", currency.getText());
        Assertions.assertEquals("CAD", decrypt(currency.getReplacement()));
        Assertions.assertEquals("1/2/20", decrypt(appliedSpan(result, FilterType.DATE).getReplacement()));

        Assertions.assertEquals(INPUT, restore(INPUT, result.getExplanation().appliedSpans(), TrimmedSpanReplacementTest::decrypt));

    }

    @Test
    void fpeEncryptReplaceRestoresTheOriginalTextOfATrimmedSpan() throws Exception {

        final FPE fpe = new FPE(FPE_KEY, FPE_TWEAK);
        final String input = "ID 12345678 87654321 end";
        final Span earlier = fpeSpan(3, 11, FilterType.SSN, input, fpe);
        final Span later = fpeSpan(7, 20, FilterType.CREDIT_CARD, input, fpe);

        final List<Span> resolved = Span.resolveOverlappingSpans(List.of(earlier, later), input);
        for (final Span span : resolved) {
            span.replaceTrimmedText();
        }

        Assertions.assertEquals("87654321", resolved.get(1).getText());
        Assertions.assertEquals(input, restore(input, resolved, replacement -> Encryption.formatPreservingDecrypt(fpe, replacement)));

    }

    private static Span fpeSpan(final int start, final int end, final FilterType filterType, final String input, final FPE fpe) {
        final String text = input.substring(start, end);
        final Span span = Span.make(start, end, filterType, "context", 0.9, text,
                Encryption.formatPreservingEncrypt(fpe, text), "", false, true, new String[0], 0);
        span.setReplacer(token -> new Replacement(Encryption.formatPreservingEncrypt(fpe, token)));
        return span;
    }

    @Test
    void fpeCannotEncryptTheTrimmedCurrencyCode() {

        // FF3 needs at least four alphanumeric characters, so the strategy redacts "CAD" instead.
        Assertions.assertThrows(FormatPreservingEncryptionException.class,
                () -> Encryption.formatPreservingEncrypt(new FPE(FPE_KEY, FPE_TWEAK), "CAD"));

    }

    @Test
    void hashSha256ReplaceHashesTheTrimmedText() throws Exception {

        final TextFilterResult result = filter(policy("HASH_SHA256_REPLACE", ""), INPUT);

        Assertions.assertEquals(DigestUtils.sha256Hex("CAD"), appliedSpan(result, FilterType.CURRENCY).getReplacement());

    }

    @Test
    void last4TakesItsCharactersFromTheTrimmedText() throws Exception {

        final TextFilterResult result = filter(policy("LAST_4", ""), INPUT);

        Assertions.assertEquals("CAD", appliedSpan(result, FilterType.CURRENCY).getReplacement());

    }

    @Test
    void theTrimmedTextKeepsTheSameReplacementWithinAContext() throws Exception {

        final ContextService contextService = new DefaultContextService();
        final String policy = policyWithStrategy("{\"strategy\": \"RANDOM_REPLACE\", \"replacementScope\": \"CONTEXT\"}", "");

        final String trimmed = appliedSpan(filter(policy, contextService, "context", INPUT), FilterType.CURRENCY).getReplacement();
        final String again = appliedSpan(filter(policy, contextService, "context", INPUT), FilterType.CURRENCY).getReplacement();

        Assertions.assertEquals(trimmed, again);
        Assertions.assertEquals(trimmed, contextService.getReplacement("CAD"));

    }

    @Test
    void aConditionTheTrimmedTextDoesNotMeetKeepsTheReplacementOfTheFullMatch() throws Exception {

        final String policy = "{\"identifiers\": {\"date\": {}, \"currency\": {\"currencyFilterStrategies\": "
                + "[{\"strategy\": \"STATIC_REPLACE\", \"staticReplacement\": \"AMOUNT\", \"condition\": \"token == \\\"20 CAD\\\"\"}]}}}";

        Assertions.assertEquals("DOB {{{REDACTED-date}}} AMOUNT stable.", filter(policy, INPUT).getFilteredText());

    }

    @Test
    void noTextCoveredByEitherSpanIsLeftInTheOutput() throws Exception {

        final String output = filter(policy("CRYPTO_REPLACE", ""), INPUT).getFilteredText();

        Assertions.assertTrue(output.startsWith("DOB {{"), output);
        Assertions.assertTrue(output.endsWith("}} stable."), output);
        Assertions.assertFalse(output.contains("1/2/20"), output);
        Assertions.assertFalse(output.contains("CAD"), output);

    }

    @Test
    void aPolicyLevelIgnoredTermEqualToTheFullMatchAppliesToATrimmedSpan() throws Exception {

        final String policy = "{\"ignored\": [{\"terms\": [\"20 CAD\"]}], \"identifiers\": {\"date\": {}, \"currency\": {}}}";

        Assertions.assertEquals("DOB {{{REDACTED-date}}} CAD stable.", filter(policy, INPUT).getFilteredText());

    }

    @Test
    void aPolicyLevelIgnoredTermEqualToTheTrimmedTextStillApplies() throws Exception {

        final String policy = "{\"ignored\": [{\"terms\": [\"CAD\"]}], \"identifiers\": {\"date\": {}, \"currency\": {}}}";

        Assertions.assertEquals("DOB {{{REDACTED-date}}} CAD stable.", filter(policy, INPUT).getFilteredText());

    }

    @Test
    void aPolicyLevelIgnoredPatternMatchingTheFullMatchAppliesToATrimmedSpan() throws Exception {

        final String policy = "{\"ignoredPatterns\": [{\"name\": \"amount\", \"pattern\": \"\\\\d+ CAD\"}], "
                + "\"identifiers\": {\"date\": {}, \"currency\": {}}}";

        Assertions.assertEquals("DOB {{{REDACTED-date}}} CAD stable.", filter(policy, INPUT).getFilteredText());

    }

}
