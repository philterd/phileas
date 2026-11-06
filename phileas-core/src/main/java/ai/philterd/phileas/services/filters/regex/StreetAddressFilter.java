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

public class StreetAddressFilter extends RegexFilter {

    // Directionals, longest forms first so a full word wins over its abbreviation.
    private static final String DIRECTIONAL =
            "(?:Northeast|Northwest|Southeast|Southwest|North|South|East|West|NE|NW|SE|SW|N|S|E|W)";

    // Street types, each long form before its abbreviation so the whole word is preferred.
    private static final String STREET_TYPE =
            "(?:Street|St|Avenue|Ave|Boulevard|Blvd|Drive|Dr|Road|Rd|Lane|Ln|Way|Court|Ct|Place|Pl|Circle|Cir" +
            "|Highway|Hwy|Parkway|Pkwy|Square|Sq|Trail|Trl|Terrace|Ter|Turnpike|Tpke|Expressway|Expy|Freeway|Fwy" +
            "|Crossing|Xing|Crescent|Cres|Plaza|Plz|Landing|Lndg|Gardens|Garden|Gdns|Commons|Manor|Mnr|Ridge|Rdg" +
            "|Point|Pt|Grove|Grv|Alley|Aly|Cove|Cv|Bend|Bnd|Loop|Pike|Path|Mews|Row|Run|Walk|Close)";

    // Unit / secondary-address designators and the unit number after them.
    private static final String UNIT_WORD = "(?:Apt|Apartment|Suite|Ste|Unit|Bldg|Building|Floor|Fl|Room|Rm)";
    private static final String UNIT_NUMBER = "\\.?[ \\t]*#?[ \\t]*[A-Za-z0-9-]+";

    // A unit folded into the span when present: on the street's line, or alone on the next line,
    // ignoring spaces and tabs around it. "Apt 4B" below the street is folded in, "Room 5 is booked"
    // is not. On the next line the designator must be a whole word, so "Steve" is not "Ste" "ve".
    private static final String UNIT =
            "(?:[, \\t]+(?:" + UNIT_WORD + "|#)" + UNIT_NUMBER +
            "|[, \\t]*(?:\\r\\n|[\\r\\n])[ \\t]*(?:" + UNIT_WORD + "(?![A-Za-z])|#)" + UNIT_NUMBER +
            "(?=[ \\t]*(?:[\\r\\n]|\\z)))?";

    public StreetAddressFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.STREET_ADDRESS, filterConfiguration);

        // A street address: a house number (optionally a range and/or a trailing letter), an optional
        // leading directional, one to five name words (ordinals, saint/abbreviated names, etc.), a
        // street type, an optional trailing directional, and an optional unit. The parts are joined by
        // spaces and tabs only, so a match does not run from one line into the next (a ZIP code at the
        // end of a line and "Dear Dr." on the next are not an address). Only a unit alone on the next
        // line is taken past the street's line.
        final Pattern addressPattern = Pattern.compile(
                "\\b\\d{1,6}(?:-\\d{1,6})?[A-Za-z]?[ \\t]+" +
                "(?:" + DIRECTIONAL + "[ \\t]+)?" +
                "(?:[A-Za-z0-9'.-]+[ \\t]+){1,5}" +
                STREET_TYPE + "\\b\\.?" +
                "(?:[ \\t]+" + DIRECTIONAL + "\\b)?" +
                UNIT,
                Pattern.CASE_INSENSITIVE);
        final FilterPattern addressFilterPattern = new FilterPattern.FilterPatternBuilder(addressPattern, 0.85).build();

        // A PO box, e.g. "PO Box 1234", "P.O. Box 56", "Post Office Box 789", on one line.
        final Pattern poBoxPattern = Pattern.compile(
                "\\b(?:P\\.?[ \\t]?O\\.?[ \\t]?Box|Post[ \\t]+Office[ \\t]+Box)[ \\t]+\\d+\\b",
                Pattern.CASE_INSENSITIVE);
        final FilterPattern poBoxFilterPattern = new FilterPattern.FilterPatternBuilder(poBoxPattern, 0.85).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("address");
        this.contextualTerms.add("location");

        this.analyzer = new Analyzer(contextualTerms, addressFilterPattern, poBoxFilterPattern);

    }

    @Override
    public Filtered filter(ContextService contextService, Policy policy, String context, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(contextService, policy, analyzer, input, context);

        return new Filtered(context, spans);

    }

}
