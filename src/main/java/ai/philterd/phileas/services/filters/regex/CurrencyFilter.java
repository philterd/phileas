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
package ai.philterd.phileas.services.filters.regex;

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.regex.RegexFilter;
import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.Analyzer;
import ai.philterd.phileas.services.context.ContextService;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class CurrencyFilter extends RegexFilter {

    private static final String NUMBER = "(?<!\\w)(?<!\\d[.,])\\d++(?:[.,]\\d++)*+";
    private static final String AMOUNT = "[0-9.,]*[0-9]";
    // Atomic so a dosage such as "2.55MG" cannot backtrack to "2.5" to get past the clinical dosage guard.
    private static final String GROUPED_OR_DECIMAL_AMOUNT = "(?>[0-9.,]*[.,][0-9.,]*[0-9])";
    private static final String MAGNITUDE_SUFFIX = "(?:(?:k|m|bn)\\b)?+";
    private static final List<String> CLINICAL_DOSAGE_UNITS = List.of(
            "mg", "mcg", "g", "kg", "ml", "l", "cc", "iu", "mmol", "meq",
            "units?", "tablets?", "tabs?", "caps?", "capsules?", "drops?", "puffs?", "patches", "doses?"
    );
    private static final String CLINICAL_DOSAGE_GUARD = "(?!\\h*+(?:" + String.join("|", CLINICAL_DOSAGE_UNITS) + ")\\b)";

    private static final List<String> ISO_CODES = List.of(
            "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "INR",
            "SEK", "NOK", "DKK", "PLN", "CZK", "HUF",
            "NZD", "CNY", "HKD", "SGD", "KRW", "MXN", "BRL", "ZAR",
            "BGN", "ISK", "UAH", "HRK"
    );

    private static final String UNAMBIGUOUS_ISO_PATTERN = "(?:" + String.join("|", ISO_CODES) + ")";

    // Codes that are also words, names, or abbreviations ("try", "Ron", "CHF" for heart failure). They are
    // uppercase only and need a comma or period in the number, so "CHF 3,000" matches and "CHF 3 years" does not.
    private static final String AMBIGUOUS_ISO_PATTERN = "(?-i:RUB|TRY|RON|CHF)";
    private static final String ISO_PATTERN = "(?:" + String.join("|", ISO_CODES) + "|" + AMBIGUOUS_ISO_PATTERN + ")";

    private static final List<String> NATIONALITIES = List.of(
            "U\\.S\\.", "US", "U\\.K\\.", "UK",
            "American", "Australian", "Austrian", "Belgian", "British", "Bulgarian",
            "Canadian", "Chinese", "Croatian", "Cypriot", "Czech", "Czechoslovak",
            "Danish", "Dutch", "Egyptian", "English", "Estonian", "European",
            "Finnish", "French", "German", "Gibraltar", "Greek", "Hungarian",
            "Icelandic", "Indian", "Irish", "Italian", "Japanese", "Lebanese",
            "Maltese", "Mexican", "Norwegian", "Polish", "Portuguese", "Romanian",
            "Russian", "Scottish", "Slovak", "Spanish", "Sudanese", "Swedish",
            "Swiss", "Syrian", "Turkish", "Ukrainian"
    );

    private static final List<String> STANDALONE_CURRENCY_NAMES = List.of(
            "pounds?\\h++sterling", "dollars?", "cents?", "euros?",
            "yen", "yuan", "rupees?", "francs?", "schillings?",
            "kronor", "kroner", "krona", "krone",
            "koruny", "korunas?", "korun", "zlotys?", "z\\u0142otys?", "forints?",
            "rubles?", "roubles?", "liras?", "lire",
            "pesetas?", "guilders?", "drachmas?", "kunas?", "hryvnias?", "pesos?", "rands?",
            "(?:German|Deutsche|Finnish)\\h++marks?"
    );

    private static final String NATIONALITY_PREFIX = "(?:new\\h++)?(?:"
            + String.join("|", NATIONALITIES) + ")\\h++(?:new\\h++)?";

    // Bare "pounds" and "crowns" commonly denote body weight or dental crowns in clinical notes,
    // so they require a nationality word in front ("British pounds", "Swedish crowns") or "pounds sterling".
    private static final String CURRENCY_NAME_PHRASE = "(?:"
            + NATIONALITY_PREFIX + "(?:pounds?(?:\\h++sterling)?|crowns?)"
            + "|(?:" + NATIONALITY_PREFIX + ")?(?:" + String.join("|", STANDALONE_CURRENCY_NAMES) + ")"
            + ")";

    public CurrencyFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.CURRENCY, filterConfiguration);

        final int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

        // Pattern 1: Prefix Symbol (e.g., €1,450.00, € 1.500,00, $35.53, £250.00, ¥5000, ₹500, $3,450.75 USD)
        final Pattern currencyPattern1 = Pattern.compile(
                "[$\\u20AC\\u00A3\\u00A5\\u20B9]\\s*[0-9.,]*[0-9]" + MAGNITUDE_SUFFIX + "(?:\\s*\\b" + ISO_PATTERN + "\\b)?",
                flags
        );
        final FilterPattern currency1 = new FilterPattern.FilterPatternBuilder(currencyPattern1, 0.80).build();

        // Pattern 2: Suffix Symbol (e.g., 1.450,00 €, 250,50 £, 5000 ¥, 500 ₹)
        final Pattern currencyPattern2 = Pattern.compile("(?<![$\\u20AC\\u00A3\\u00A5\\u20B9])[0-9.,]*[0-9]\\s*[$\\u20AC\\u00A3\\u00A5\\u20B9](?!\\d)", Pattern.CASE_INSENSITIVE);
        final FilterPattern currency2 = new FilterPattern.FilterPatternBuilder(currencyPattern2, 0.80).build();

        // Pattern 3: Unambiguous Prefix ISO Code (e.g., GBP 250.00, EUR 3,000, USD 1.5M, USD .50)
        final Pattern currencyPattern3 = Pattern.compile("\\b" + UNAMBIGUOUS_ISO_PATTERN + "\\b\\s*+" + AMOUNT + MAGNITUDE_SUFFIX, flags);
        final FilterPattern currency3 = new FilterPattern.FilterPatternBuilder(currencyPattern3, 0.80).build();

        // Pattern 4: Ambiguous Prefix ISO Code requiring a comma or period in the number (e.g., TRY 3,000, RUB 250.00)
        final Pattern currencyPattern4 = Pattern.compile(
                "\\b" + AMBIGUOUS_ISO_PATTERN + "\\b\\s*+" + GROUPED_OR_DECIMAL_AMOUNT + MAGNITUDE_SUFFIX + CLINICAL_DOSAGE_GUARD,
                flags
        );
        final FilterPattern currency4 = new FilterPattern.FilterPatternBuilder(currencyPattern4, 0.80).build();

        // Pattern 5: Unambiguous Suffix ISO Code, including leading decimals and codes on the next line (e.g., .50 USD, 500 EUR, 235,000 SEK)
        final Pattern currencyPattern5 = Pattern.compile(AMOUNT + MAGNITUDE_SUFFIX + "\\s*+\\b" + UNAMBIGUOUS_ISO_PATTERN + "\\b", flags);
        final FilterPattern currency5 = new FilterPattern.FilterPatternBuilder(currencyPattern5, 0.80).build();

        // Pattern 6: Ambiguous Suffix ISO Code requiring a comma or period in the number (e.g., 3,000 TRY, 250.00 CHF)
        final Pattern currencyPattern6 = Pattern.compile(GROUPED_OR_DECIMAL_AMOUNT + MAGNITUDE_SUFFIX + "\\s*+\\b" + AMBIGUOUS_ISO_PATTERN + "\\b", flags);
        final FilterPattern currency6 = new FilterPattern.FilterPatternBuilder(currencyPattern6, 0.80).build();

        // Pattern 7: Amount followed by a currency name and optional nationality adjective (e.g., 235,000 Swedish crowns, 5,000 euros)
        final Pattern currencyPattern7 = Pattern.compile(NUMBER + "\\h++" + CURRENCY_NAME_PHRASE + "\\b", flags);
        final FilterPattern currency7 = new FilterPattern.FilterPatternBuilder(currencyPattern7, 0.80).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("dollars");
        this.contextualTerms.add("amount");
        this.contextualTerms.add("euros");
        this.contextualTerms.add("pounds");
        this.contextualTerms.add("yen");
        this.contextualTerms.add("rupees");
        this.contextualTerms.add("currency");
        this.contextualTerms.add("price");
        this.contextualTerms.add("cost");
        this.contextualTerms.add("fee");
        this.contextualTerms.add("balance");

        this.analyzer = new Analyzer(contextualTerms, currency1, currency2, currency3, currency4, currency5, currency6, currency7);

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        return new Filtered(context, nonOverlappingSpans);

    }

}
