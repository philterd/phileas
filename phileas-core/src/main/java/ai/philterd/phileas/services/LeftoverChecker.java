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
package ai.philterd.phileas.services;

import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.model.filtering.LeftoverFinding;
import ai.philterd.phileas.model.filtering.ReplacedRange;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.LeftoverCheck;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.filtering.FilterOptions;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The policy's leftover check: a second detection pass over redacted output that reports what is still
 * detectable. The check runs with a policy derived from the redaction policy (see {@link #checkPolicy}) and
 * never changes the output.
 */
public final class LeftoverChecker {

    private static final Gson GSON = new Gson();

    // The context name the check's filters see. The check uses a throwaway context service, so no
    // referential-integrity state of the caller's context is read or written.
    private static final String CONTEXT = "leftover-check";

    /**
     * The identifiers that are one built-in filter each, keyed by policy key, with the key of their strategies.
     * {@code broad} runs each of these at its defaults when the policy does not enable it.
     */
    static final Map<String, String> BUILT_IN_FILTERS = new LinkedHashMap<>();

    /**
     * The identifiers that need configuration to run (lists, and model-backed filters), with the key of their
     * strategies. The check runs them only as the policy defines them.
     */
    static final Map<String, String> CONFIGURED_FILTERS = new LinkedHashMap<>();

    static {
        BUILT_IN_FILTERS.put("age", "ageFilterStrategies");
        BUILT_IN_FILTERS.put("bankRoutingNumber", "bankRoutingNumberFilterStrategies");
        BUILT_IN_FILTERS.put("bitcoinAddress", "bitcoinFilterStrategies");
        BUILT_IN_FILTERS.put("canadaSin", "canadaSinFilterStrategies");
        BUILT_IN_FILTERS.put("city", "cityFilterStrategies");
        BUILT_IN_FILTERS.put("county", "countyFilterStrategies");
        BUILT_IN_FILTERS.put("creditCard", "creditCardFilterStrategies");
        BUILT_IN_FILTERS.put("currency", "currencyFilterStrategies");
        BUILT_IN_FILTERS.put("date", "dateFilterStrategies");
        BUILT_IN_FILTERS.put("deaNumber", "deaNumberFilterStrategies");
        BUILT_IN_FILTERS.put("deviceIdentifier", "deviceIdentifierFilterStrategies");
        BUILT_IN_FILTERS.put("driversLicense", "driversLicenseFilterStrategies");
        BUILT_IN_FILTERS.put("ein", "einFilterStrategies");
        BUILT_IN_FILTERS.put("emailAddress", "emailAddressFilterStrategies");
        BUILT_IN_FILTERS.put("firstName", "firstNameFilterStrategies");
        BUILT_IN_FILTERS.put("healthPlanMemberId", "healthPlanMemberIdFilterStrategies");
        BUILT_IN_FILTERS.put("hospital", "hospitalFilterStrategies");
        BUILT_IN_FILTERS.put("ibanCode", "ibanCodeFilterStrategies");
        BUILT_IN_FILTERS.put("ipAddress", "ipAddressFilterStrategies");
        BUILT_IN_FILTERS.put("itin", "itinFilterStrategies");
        BUILT_IN_FILTERS.put("macAddress", "macAddressFilterStrategies");
        BUILT_IN_FILTERS.put("mbi", "mbiFilterStrategies");
        BUILT_IN_FILTERS.put("npi", "npiFilterStrategies");
        BUILT_IN_FILTERS.put("passportNumber", "passportNumberFilterStrategies");
        BUILT_IN_FILTERS.put("phoneNumber", "phoneNumberFilterStrategies");
        BUILT_IN_FILTERS.put("phoneNumberExtension", "phoneNumberExtensionFilterStrategies");
        BUILT_IN_FILTERS.put("ssn", "ssnFilterStrategies");
        BUILT_IN_FILTERS.put("state", "stateFilterStrategies");
        BUILT_IN_FILTERS.put("stateAbbreviation", "stateAbbreviationFilterStrategies");
        BUILT_IN_FILTERS.put("streetAddress", "streetAddressFilterStrategies");
        BUILT_IN_FILTERS.put("surname", "surnameFilterStrategies");
        BUILT_IN_FILTERS.put("trackingNumber", "trackingNumberFilterStrategies");
        BUILT_IN_FILTERS.put("url", "urlFilterStrategies");
        BUILT_IN_FILTERS.put("vin", "vinFilterStrategies");
        BUILT_IN_FILTERS.put("zipCode", "zipCodeFilterStrategies");

        CONFIGURED_FILTERS.put("dictionaries", "customFilterStrategies");
        CONFIGURED_FILTERS.put("identifiers", "identifierFilterStrategies");
        CONFIGURED_FILTERS.put("person", "phEyeFilterStrategies");
        CONFIGURED_FILTERS.put("pheyes", "phEyeFilterStrategies");
        CONFIGURED_FILTERS.put("sections", "sectionFilterStrategies");
    }

    private LeftoverChecker() {
        // Access the members of this class through its static methods.
    }

    /**
     * Whether the policy calls for the check on this call.
     * @param policy The {@link Policy} the call is filtered with.
     * @param options The caller's {@link FilterOptions}.
     * @return <code>true</code> if the check runs.
     */
    public static boolean isDue(final Policy policy, final FilterOptions options) {

        if (policy.getConfig() == null || policy.getConfig().getReview() == null) {
            return false;
        }

        return switch (policy.getConfig().getReview().getLeftoverCheck().getMode()) {
            case ALWAYS -> true;
            case ON_REQUEST -> options != null && options.isLeftoverCheckRequested();
            case OFF -> false;
        };

    }

    /**
     * The policy the check detects with. Each filter the policy defines keeps its settings, without its own
     * ignore lists ({@code ignored}, {@code ignoredFiles}, {@code ignoredPatterns}), and with a single
     * {@code REDACT} strategy, so a strategy condition that leaves a value unredacted does not hide it from the
     * check and no anonymization (encryption, replacement generators) is run. The policy's top-level ignore
     * lists and post-filters are kept. With {@link LeftoverCheck.Using#BROAD}, every built-in filter the policy
     * does not enable is added at its defaults.
     * @param policy The redaction {@link Policy}.
     * @param using What the check detects with.
     * @return The {@link Policy} for the check.
     */
    public static Policy checkPolicy(final Policy policy, final LeftoverCheck.Using using) {

        final JsonObject json = GSON.toJsonTree(policy).getAsJsonObject();

        // Not used by text detection, and replacement generators would only be built to go unused.
        json.remove("generators");
        json.remove("graphical");
        json.remove("metadata");

        if (json.has("config") && json.get("config").isJsonObject()) {
            json.getAsJsonObject("config").remove("review");
        }

        final JsonObject identifiers = json.has("identifiers") && json.get("identifiers").isJsonObject()
                ? json.getAsJsonObject("identifiers") : new JsonObject();

        for (final Map.Entry<String, JsonElement> entry : identifiers.entrySet()) {

            final String strategiesKey = strategiesKey(entry.getKey());

            if (strategiesKey == null) {
                continue;
            }

            if (entry.getValue().isJsonObject()) {
                asWritten(entry.getValue().getAsJsonObject(), strategiesKey);
            } else if (entry.getValue().isJsonArray()) {
                for (final JsonElement element : entry.getValue().getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        asWritten(element.getAsJsonObject(), strategiesKey);
                    }
                }
            }

        }

        if (using == LeftoverCheck.Using.BROAD) {
            for (final Map.Entry<String, String> builtIn : BUILT_IN_FILTERS.entrySet()) {
                if (!isEnabled(identifiers.get(builtIn.getKey()))) {
                    final JsonObject defaults = new JsonObject();
                    defaults.add(builtIn.getValue(), redactStrategies());
                    identifiers.add(builtIn.getKey(), defaults);
                }
            }
        }

        json.add("identifiers", identifiers);

        return GSON.fromJson(json, Policy.class);

    }

    /**
     * Runs the check's filters over text and returns what they find, in document order. Findings the policy's
     * top-level ignore lists remove are left out.
     * @param checkPolicy The policy from {@link #checkPolicy}.
     * @param filters The filters of {@code checkPolicy}.
     * @param postFilters The post-filters of {@code checkPolicy}.
     * @param text The redacted text.
     * @return The spans found, with offsets into {@code text}.
     * @throws Exception Thrown if a filter fails.
     */
    public static List<Span> detect(final Policy checkPolicy, final List<Filter> filters,
                                    final List<PostFilter> postFilters, final String text) throws Exception {

        final ContextService contextService = new DefaultContextService();

        List<Span> spans = new ArrayList<>();

        for (final Filter filter : filters) {
            spans.addAll(filter.filter(contextService, checkPolicy, CONTEXT, 0, text).getSpans());
        }

        // As the redaction does: one span per stretch of text.
        spans = Span.resolveOverlappingSpans(spans, text);

        for (final PostFilter postFilter : postFilters) {
            if (!postFilter.skipped()) {
                spans = postFilter.filter(text, spans);
            }
        }

        // Every filter in the check has an unconditional strategy, so a span left unapplied or ignored here
        // was rejected by the filter's own detection settings, such as a zip code that fails validation.
        return spans.stream()
                .filter(Span::isApplied)
                .filter(span -> !span.isIgnored())
                .sorted(Comparator.comparingInt(Span::getCharacterStart))
                .toList();

    }

    /**
     * Whether a span lies wholly in text the redaction inserted, such as a {@code RANDOM_REPLACE} stand-in.
     * @param span The span.
     * @param replacedRanges Where the redaction put its replacements.
     * @return <code>true</code> if the span is inserted text.
     */
    public static boolean isInserted(final Span span, final List<ReplacedRange> replacedRanges) {
        for (final ReplacedRange range : replacedRanges) {
            if (range.contains(span.getCharacterStart(), span.getCharacterEnd())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The finding for a span of plain text.
     * @param span The span.
     * @param text The text the span's offsets refer to.
     * @param options The caller's {@link FilterOptions}.
     * @return The {@link LeftoverFinding}.
     */
    public static LeftoverFinding finding(final Span span, final String text, final FilterOptions options) {
        return finding(span, text, options, 0, 0, 0);
    }

    /**
     * The finding for a span, with its place in a document.
     * @param span The span.
     * @param text The text the span's offsets refer to.
     * @param options The caller's {@link FilterOptions}.
     * @param pageNumber The page, for a PDF.
     * @param lineNumber The line, for a PDF.
     * @param paragraphNumber The paragraph or cell, for a Word or Excel document.
     * @return The {@link LeftoverFinding}.
     */
    public static LeftoverFinding finding(final Span span, final String text, final FilterOptions options,
                                          final int pageNumber, final int lineNumber, final int paragraphNumber) {

        final String matchedText = options != null && options.isLeftoverCheckMatchedText()
                ? text.substring(span.getCharacterStart(), span.getCharacterEnd()) : null;

        return new LeftoverFinding(span.getFilterType(), span.getClassification(), span.getCharacterStart(),
                span.getCharacterEnd(), span.getConfidence(), pageNumber, lineNumber, paragraphNumber, matchedText);

    }

    private static String strategiesKey(final String identifier) {
        final String key = BUILT_IN_FILTERS.get(identifier);
        return key != null ? key : CONFIGURED_FILTERS.get(identifier);
    }

    // The filter as the policy wrote it, without its own ignore lists and with an unconditional strategy.
    private static void asWritten(final JsonObject filter, final String strategiesKey) {
        filter.remove("ignored");
        filter.remove("ignoredFiles");
        filter.remove("ignoredPatterns");
        filter.add(strategiesKey, redactStrategies());
    }

    private static JsonArray redactStrategies() {
        final JsonObject strategy = new JsonObject();
        strategy.addProperty("strategy", AbstractFilterStrategy.REDACT);
        final JsonArray strategies = new JsonArray();
        strategies.add(strategy);
        return strategies;
    }

    private static boolean isEnabled(final JsonElement filter) {
        if (filter == null || !filter.isJsonObject()) {
            return false;
        }
        final JsonElement enabled = filter.getAsJsonObject().get("enabled");
        return enabled == null || !enabled.isJsonPrimitive() || enabled.getAsBoolean();
    }

}
