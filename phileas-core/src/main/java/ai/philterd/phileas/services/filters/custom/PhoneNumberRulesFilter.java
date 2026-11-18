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
package ai.philterd.phileas.services.filters.custom;

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.RulesFilter;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.PhoneNumber;
import ai.philterd.phileas.services.context.ContextService;
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: This should not extend RulesFilter because it is not a rule-based filter.

public class PhoneNumberRulesFilter extends RulesFilter {

    private final PhoneNumberUtil phoneUtil;
    private final List<String> regions;
    private final PhoneNumberUtil.Leniency leniency;
    private final Pattern pattern = Pattern.compile("^(\\+\\d{1,2}\\s)?\\(?\\d{3}\\)?[\\s.-]\\d{3}[\\s.-]\\d{4}$");

    // A bare "1" country code followed by a separator and a phone-formatted number, such as "1-112-997-5633".
    // Without a "+", libphonenumber strips the "1" as the country code and then strips an area code's leading
    // "1" as a national prefix, leaving too few digits for a possible number, so the match is dropped.
    private static final Pattern BARE_COUNTRY_CODE = Pattern.compile("(?<![\\d+])1[-. ](?=\\(?\\d{3}\\)?[-. ]\\d{3}[-. ]\\d{4})");

    // A period after a digit and before whitespace, including a non-breaking space. libphonenumber reads
    // it as a separator, so "123-45-6789. 57" would be one number running into the next sentence.
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=\\d)\\.(?=[\\s\\u00A0])");

    // The digits a group needs before a period for the period to end a number rather than separate its
    // groups, as in "412. 555. 0187". Seven is the length of a North American local number.
    private static final int MIN_DIGITS_BEFORE_SENTENCE_END = 7;

    // The first configured region that uses country code 1, or null when none does.
    private final String northAmericanRegion;

    public PhoneNumberRulesFilter(final FilterConfiguration filterConfiguration) {
        this(filterConfiguration, List.of(PhoneNumber.DEFAULT_REGION));
    }

    public PhoneNumberRulesFilter(final FilterConfiguration filterConfiguration, final List<String> regions) {

        this(filterConfiguration, regions, PhoneNumberUtil.Leniency.POSSIBLE);

    }

    public PhoneNumberRulesFilter(final FilterConfiguration filterConfiguration, final List<String> regions,
                                  final PhoneNumberUtil.Leniency leniency) {

        super(FilterType.PHONE_NUMBER, filterConfiguration);

        this.phoneUtil = PhoneNumberUtil.getInstance();
        this.leniency = Objects.requireNonNull(leniency, "leniency");
        this.regions = (regions == null || regions.isEmpty()) ? List.of(PhoneNumber.DEFAULT_REGION) : regions;
        this.northAmericanRegion = this.regions.stream()
                .filter(region -> phoneUtil.getCountryCodeForRegion(region) == 1)
                .findFirst()
                .orElse(null);

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("phone");
        this.contextualTerms.add("telephone");
        this.contextualTerms.add("fax");
        this.contextualTerms.add("cell");
        this.contextualTerms.add("mobile");

    }

    @Override
    public Filtered filter(final ContextService contextService, final Policy policy, final String context, final int piece,
                           final String input) throws Exception {

        final List<Span> spans = new LinkedList<>();

        if(policy.getIdentifiers().hasFilter(filterType)) {

            // Numbers are found in a same-length copy of the input with sentence-ending periods replaced by ";",
            // and in the input itself, because cutting a run of numbers can change how the rest of it is split.
            final String scanned = SENTENCE_END.matcher(input).replaceAll(";");

            // A third copy is cut only at a period that follows a whole number's worth of digits counted across
            // the groups before it, so "412. 555. 0187. 57" is read as "412. 555. 0187" and not cut into its groups.
            final String cutAtNumberEnds = cutAtNumberEnds(input);
            final List<PhoneNumberMatch> cutAtNumberEndsMatches = new ArrayList<>();

            // Scan once per configured region and merge the results. A "+"-prefixed number is found under
            // every region and a bare national-format number may match in several, so overlapping spans are
            // de-duplicated below, preferring valid numbers over merely-possible ones and longer over shorter.
            final List<PhoneNumberMatch> allMatches = new ArrayList<>();
            for (final String region : regions) {
                for (final PhoneNumberMatch match : phoneUtil.findNumbers(scanned, region, leniency, Long.MAX_VALUE)) {
                    allMatches.add(match);
                }
                if (!scanned.equals(input)) {
                    for (final PhoneNumberMatch match : phoneUtil.findNumbers(input, region, leniency, Long.MAX_VALUE)) {
                        if (!crossesSentenceEnd(match, input)) {
                            allMatches.add(match);
                        }
                    }
                }
                if (!cutAtNumberEnds.equals(scanned) && !cutAtNumberEnds.equals(input)) {
                    for (final PhoneNumberMatch match : phoneUtil.findNumbers(cutAtNumberEnds, region, leniency, Long.MAX_VALUE)) {
                        cutAtNumberEndsMatches.add(match);
                    }
                }
            }

            // A match from that copy is only kept where it takes in whole every match already found that it
            // overlaps, as "412. 555-0187" does "555-0187", so numbers found before keep their span otherwise.
            final List<PhoneNumberMatch> foundMatches = new ArrayList<>(allMatches);
            for (final PhoneNumberMatch match : cutAtNumberEndsMatches) {
                if (foundMatches.stream().allMatch(m -> m.end() <= match.start() || match.end() <= m.start()
                        || (match.start() <= m.start() && m.end() <= match.end()))) {
                    allMatches.add(match);
                }
            }

            // Also scan a copy of the text in which a bare "1" country code becomes "+1" so numbers such as
            // "1-112-997-5633" are found the same as "+1 112-997-5633". "1-" and "+1" are the same length, so
            // offsets in the copy are offsets in the input. This is done once, and only when a configured region
            // uses country code 1, so a policy for another region does not read these as North American numbers.
            // A match from the copy is only kept where the input itself had no match, so numbers already found,
            // such as "112-997-5633" in "Item 1 112-997-5633", keep their span and confidence.
            if (northAmericanRegion != null) {
                final String rewritten = BARE_COUNTRY_CODE.matcher(scanned).replaceAll("+1");
                if (!rewritten.equals(scanned)) {
                    final List<PhoneNumberMatch> inputMatches = new ArrayList<>(allMatches);
                    for (final PhoneNumberMatch match : phoneUtil.findNumbers(rewritten, northAmericanRegion, leniency, Long.MAX_VALUE)) {
                        if (inputMatches.stream().noneMatch(m -> match.start() < m.end() && m.start() < match.end())) {
                            allMatches.add(match);
                        }
                    }
                }
            }

            for (final PhoneNumberMatch match : dedupe(allMatches)) {

                // Taken from the input rather than rawString(), which is the rewritten text for a match found in a copy.
                final String text = input.substring(match.start(), match.end());

                // Is it formatted like a phone number?
                double confidence;
                if(text.matches(pattern.pattern())) {
                    confidence = 0.95;
                } else{
                    if(text.length() > 14) {
                        confidence = 0.75;
                    } else {
                        confidence = 0.60;
                    }
                }

                final String[] window = getWindow(input, match.start(), match.end());
                final String classification = "";
                final Replacement replacement = getReplacement(contextService, policy, context, text, window, confidence,
                        classification, null);
                final boolean isIgnored = isIgnored(text);

                final Span span = Span.make(match.start(), match.end(), getFilterType(), context, confidence,
                        text, replacement.getReplacement(), replacement.getSalt(), isIgnored, replacement.isApplied(), window, priority);

                span.setColor(replacement.getColor());
                spans.add(span);

            }

        }

        return new Filtered(context, spans);

    }

    /**
     * Returns a same-length copy of the input in which a period after a digit and before whitespace is replaced
     * by ";" when the digits before it, counted back across separators to the start of the number or the last
     * replaced period, are a whole number's worth. A country code after a "+" is not counted, nor is a leading 1
     * when a region is North American. In
     * "412. 555. 0187. 57" only the period after "0187" is replaced.
     */
    private String cutAtNumberEnds(final String input) {

        final BitSet sentenceEnds = new BitSet(input.length());
        final Matcher matcher = SENTENCE_END.matcher(input);
        while (matcher.find()) {
            sentenceEnds.set(matcher.start());
        }

        final char[] chars = input.toCharArray();
        final StringBuilder digits = new StringBuilder();
        boolean international = false;

        for (int i = 0; i < chars.length; i++) {

            final char c = chars[i];

            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (c == '+' && digits.length() == 0) {
                international = true;
            } else if (sentenceEnds.get(i)) {
                // A leading 1 in a North American region is the country code, since no area code starts with 1.
                final int countryCodeLength = international ? countryCodeLength(digits)
                        : (northAmericanRegion != null && digits.length() > 0 && digits.charAt(0) == '1' ? 1 : 0);
                if (digits.length() - countryCodeLength >= MIN_DIGITS_BEFORE_SENTENCE_END) {
                    chars[i] = ';';
                    digits.setLength(0);
                    international = false;
                }
            } else if (!isSeparator(c)) {
                digits.setLength(0);
                international = false;
            }

        }

        return new String(chars);

    }

    private static boolean isSeparator(final char c) {
        return Character.isWhitespace(c) || c == '\u00A0' || c == '.' || c == '-' || c == '(' || c == ')' || c == '/';
    }

    /**
     * The number of leading digits that are a country code. Country codes are prefix-free, so the first
     * one to three digits that are a known calling code are the country code.
     */
    private int countryCodeLength(final CharSequence digits) {

        for (int length = 1; length <= 3 && length <= digits.length(); length++) {
            final int countryCode = Integer.parseInt(digits.subSequence(0, length).toString());
            if (phoneUtil.getSupportedCallingCodes().contains(countryCode)) {
                return length;
            }
        }

        return 0;

    }

    /**
     * Whether a match found in the input runs past a period that ends a number, which is one with a whole
     * number's worth of digits before it. Otherwise the period is one of the number's separators.
     */
    private boolean crossesSentenceEnd(final PhoneNumberMatch match, final String input) {

        final String text = input.substring(match.start(), match.end());
        final String[] groups = SENTENCE_END.split(text, -1);

        for (int i = 0; i < groups.length - 1; i++) {
            long digits = groups[i].chars().filter(Character::isDigit).count();
            if (i == 0 && text.startsWith("+")) {
                digits -= String.valueOf(match.number().getCountryCode()).length();
            }
            if (digits >= MIN_DIGITS_BEFORE_SENTENCE_END) {
                return true;
            }
        }

        return false;

    }

    /**
     * Merges matches found across the configured regions, removing overlapping spans. When two matches
     * overlap the "better" one is kept: a valid number is preferred over a merely-possible one, and among
     * equally-valid matches the longer span wins.
     */
    private List<PhoneNumberMatch> dedupe(final List<PhoneNumberMatch> matches) {

        // Best-first ordering so a greedy sweep keeps the strongest match in each overlapping cluster.
        final List<PhoneNumberMatch> sorted = new ArrayList<>(matches);
        sorted.sort((a, b) -> {
            final boolean aValid = phoneUtil.isValidNumber(a.number());
            final boolean bValid = phoneUtil.isValidNumber(b.number());
            if (aValid != bValid) {
                return aValid ? -1 : 1;
            }
            final int aLength = a.end() - a.start();
            final int bLength = b.end() - b.start();
            if (aLength != bLength) {
                return Integer.compare(bLength, aLength);
            }
            return Integer.compare(a.start(), b.start());
        });

        final List<PhoneNumberMatch> kept = new ArrayList<>();
        for (final PhoneNumberMatch candidate : sorted) {
            boolean overlaps = false;
            for (final PhoneNumberMatch accepted : kept) {
                if (candidate.start() < accepted.end() && accepted.start() < candidate.end()) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                kept.add(candidate);
            }
        }

        // Restore document order so emitted spans are left-to-right.
        kept.sort((a, b) -> Integer.compare(a.start(), b.start()));

        return kept;

    }

}
