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
import ai.philterd.phileas.model.filtering.Span;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A filter whose values have no fixed format, so a value counts only when a cue word comes within
 * {@code windowSize} tokens before it. The policy can replace the default value pattern and add to or
 * replace the default cue words. Where values of two such filters overlap, the one whose cue word is nearer
 * the value is kept.
 */
public abstract class CueWordFilter extends RegexFilter {

    /**
     * The filter types a value found by a cue word gives way to where they overlap: each of these is
     * validated by its format or check digit, so it is the better reading of the text.
     */
    public static final Set<FilterType> YIELDS_TO = EnumSet.of(FilterType.MBI, FilterType.SSN, FilterType.NPI,
            FilterType.PHONE_NUMBER, FilterType.DATE);

    protected final CueWords cueWords;

    protected CueWordFilter(final FilterType filterType, final FilterConfiguration filterConfiguration,
                            final Collection<String> defaultCueWords, final Collection<String> cueWords,
                            final boolean useDefaultCueWords) {
        super(filterType, filterConfiguration);

        if(!useDefaultCueWords && (cueWords == null || cueWords.isEmpty())) {
            throw new IllegalArgumentException("The " + filterType.getType() + " filter sets defaultCueWords "
                    + "to false with no cueWords, so no value could follow a cue word.");
        }

        this.cueWords = new CueWords(defaultCueWords, cueWords, useDefaultCueWords);

    }

    /**
     * The filter patterns for the value: the policy's patterns, which replace the default, or the default.
     */
    protected static List<FilterPattern> valuePatterns(final String defaultPattern, final List<String> patterns) {

        final List<String> regexes = patterns == null || patterns.isEmpty() ? List.of(defaultPattern) : patterns;
        final List<FilterPattern> filterPatterns = new ArrayList<>();

        for(final String regex : regexes) {
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(Pattern.compile(regex), 0.90).build());
        }

        return filterPatterns;

    }

    /**
     * Whether a cue word comes within windowSize tokens before the value.
     */
    protected boolean followsCueWord(final String input, final int characterStart) {
        return cueWords.precede(input, characterStart, windowSize);
    }

    /**
     * Marks a span as found by a cue word: it gives way to an overlapping validated identifier, and to an
     * overlapping value of another cue word filter whose cue word is nearer.
     */
    protected void foundByCueWord(final String input, final Span span) {
        span.setYieldsTo(YIELDS_TO);
        span.setCue(cueWords.nearest(input, span.getCharacterStart(), windowSize));
    }

}
