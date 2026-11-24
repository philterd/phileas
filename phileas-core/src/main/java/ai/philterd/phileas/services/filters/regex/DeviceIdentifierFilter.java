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
package ai.philterd.phileas.services.filters.regex;

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.Analyzer;
import ai.philterd.phileas.services.context.ContextService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects medical device identifiers in three forms:
 * <ul>
 *     <li>A GS1 UDI in its human-readable form, such as {@code (01)00812345000016(17)270128(21)SN0042}:
 *     {@code (01)} and a 14-digit GTIN with a valid GS1 check digit, with any {@code (11)} or {@code (17)}
 *     date, {@code (10)} lot, and {@code (21)} serial that follow it.</li>
 *     <li>A HIBCC UDI, such as {@code +H123ABC01C}: {@code +}, a four-character labeler code starting with
 *     a letter, the product number, a one-digit unit of measure, and a valid mod-43 check character.</li>
 *     <li>A serial number after a cue word such as {@code S/N}. An overlapping MBI, SSN, NPI, phone
 *     number, or date takes priority over it.</li>
 * </ul>
 * The policy's patterns replace the serial number pattern; the UDI checks always apply.
 */
public class DeviceIdentifierFilter extends CueWordFilter {

    /** 4 to 20 letters, digits, and hyphens, with at least one digit. */
    public static final String DEFAULT_PATTERN =
            "(?<![A-Za-z0-9-])(?=[A-Za-z0-9-]{0,19}[0-9])[A-Za-z0-9-]{4,20}(?![A-Za-z0-9-])";

    public static final List<String> DEFAULT_CUE_WORDS = List.of("serial number", "serial no.", "serial #",
            "S/N", "SN", "device ID");

    // A lot or serial is 1 to 20 characters, starting and ending with a letter or digit.
    private static final String GS1_TEXT = "[A-Za-z0-9](?:[A-Za-z0-9\\-./]{0,18}[A-Za-z0-9])?";

    private static final Pattern GS1_PATTERN = Pattern.compile("(?<![\\w)])\\(01\\)\\d{14}"
            + "(?:\\((?:11|17)\\)\\d{6}|\\((?:10|21)\\)" + GS1_TEXT + ")*");

    private static final Pattern HIBCC_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9+])\\+[A-Z][A-Z0-9]{3}[A-Z0-9]{1,18}[0-9][0-9A-Z\\-.$/+%](?![A-Za-z0-9])");

    /** The HIBCC character set, each character's value its position. */
    private static final String HIBCC_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%";

    private final FilterPattern gs1;
    private final FilterPattern hibcc;

    public DeviceIdentifierFilter(final FilterConfiguration filterConfiguration) {
        this(filterConfiguration, null, null, true);
    }

    public DeviceIdentifierFilter(final FilterConfiguration filterConfiguration, final List<String> patterns,
                                  final List<String> cueWords, final boolean defaultCueWords) {
        super(FilterType.DEVICE_IDENTIFIER, filterConfiguration, DEFAULT_CUE_WORDS, cueWords, defaultCueWords);

        this.gs1 = new FilterPattern.FilterPatternBuilder(GS1_PATTERN, 0.90).build();
        this.hibcc = new FilterPattern.FilterPatternBuilder(HIBCC_PATTERN, 0.90).build();

        final List<FilterPattern> filterPatterns = new ArrayList<>();
        filterPatterns.add(gs1);
        filterPatterns.add(hibcc);
        filterPatterns.addAll(valuePatterns(DEFAULT_PATTERN, patterns));

        this.contextualTerms = new HashSet<>();
        this.analyzer = new Analyzer(contextualTerms, filterPatterns.toArray(new FilterPattern[0]));

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        // A serial number gives way to a validated identifier of another filter. A UDI, found by its
        // check digit or character, does not.
        for(final Span span : spans) {
            if(!isUdi(span.getText())) {
                foundByCueWord(input, span);
            }
        }

        // A serial number pattern can match part of a UDI, such as its GTIN; the whole UDI is kept.
        return new Filtered(context, Span.dropOverlappingSpans(spans));

    }

    @Override
    protected boolean accepts(final String input, final int characterStart, final int characterEnd,
                              final FilterPattern filterPattern) {

        if(filterPattern == gs1) {
            // The GTIN follows "(01)".
            return isValidGtin(input.substring(characterStart + 4, characterStart + 18));
        } else if(filterPattern == hibcc) {
            return isValidHibcc(input.substring(characterStart, characterEnd));
        } else {
            return followsCueWord(input, characterStart);
        }

    }

    /**
     * Whether a value is a GS1 or HIBCC UDI that passes its check.
     * @param value The value.
     * @return {@code true} if the value is a UDI.
     */
    public static boolean isUdi(final String value) {

        if(value == null) {
            return false;
        }

        if(GS1_PATTERN.matcher(value).matches()) {
            return isValidGtin(value.substring(4, 18));
        }

        return HIBCC_PATTERN.matcher(value).matches() && isValidHibcc(value);

    }

    /**
     * Whether a 14-digit GTIN's last digit is its GS1 mod-10 check digit.
     * @param gtin The GTIN.
     * @return {@code true} if the check digit is valid.
     */
    public static boolean isValidGtin(final String gtin) {

        if(gtin == null || !gtin.matches("\\d{14}")) {
            return false;
        }

        return gtinCheckDigit(gtin.substring(0, 13)) == gtin.charAt(13) - '0';

    }

    /**
     * The GS1 mod-10 check digit for the first 13 digits of a GTIN-14.
     * @param digits The first 13 digits.
     * @return The check digit.
     */
    public static int gtinCheckDigit(final String digits) {

        int sum = 0;

        // From the right, digits are weighted 3, 1, 3, 1, and so on.
        for(int i = 0; i < digits.length(); i++) {
            final int digit = digits.charAt(digits.length() - 1 - i) - '0';
            sum += i % 2 == 0 ? digit * 3 : digit;
        }

        return (10 - sum % 10) % 10;

    }

    /**
     * Whether a HIBCC UDI's last character is the mod-43 check character of everything before it.
     * @param value The UDI, starting with {@code +}.
     * @return {@code true} if the check character is valid.
     */
    public static boolean isValidHibcc(final String value) {

        if(value == null || value.length() < 2) {
            return false;
        }

        final Character check = hibccCheckCharacter(value.substring(0, value.length() - 1));

        return check != null && check == value.charAt(value.length() - 1);

    }

    /**
     * The HIBCC mod-43 check character for a UDI without its check character.
     * @param data The UDI without its check character.
     * @return The check character, or null if the data has a character outside the HIBCC set.
     */
    public static Character hibccCheckCharacter(final String data) {

        int sum = 0;

        for(int i = 0; i < data.length(); i++) {
            final int value = HIBCC_CHARACTERS.indexOf(data.charAt(i));
            if(value < 0) {
                return null;
            }
            sum += value;
        }

        return HIBCC_CHARACTERS.charAt(sum % 43);

    }

}
