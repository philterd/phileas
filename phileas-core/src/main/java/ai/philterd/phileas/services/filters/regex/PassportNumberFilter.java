package ai.philterd.phileas.services.filters.regex;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.regex.RegexFilter;
import ai.philterd.phileas.model.objects.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.FilterProfile;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class PassportNumberFilter extends RegexFilter {

    public PassportNumberFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.PASSPORT_NUMBER, filterConfiguration);

        // https://www.e-verify.gov/about-e-verify/e-verify-data/e-verify-enhancements/june-2011
        // U.S. Passport numbers must be between six and nine alphanumeric characters (letters and numbers).
        // The "C" that precedes a U.S. Passport Card number is no longer case sensitive.
        // U.S. visa numbers must be exactly eight alphanumeric characters (letters and numbers). Entering a visa number is still optional though if an employee provides one, we encourage you to enter it, as doing so may prevent a tentative nonconfirmation (TNC).

        // U.S. Passports Issued: 1981-Current
        // PHL-123: Set the regex such that the initial two digits are valid and not just 0-9.
        // See the chart at https://passportinfo.com/blog/what-is-my-passport-number/.

        final FilterPattern passportUS01 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(01)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS02 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(02)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS03 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(03)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS04 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(04)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS05 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(05)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS06 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(06)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS07 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(07)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS08 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(08)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS09 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(09)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS10 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(10)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS11 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(11)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS12 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(12)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS13 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(13)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS14 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(14)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS15 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(15)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS16 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(16)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS17 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(17)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS20 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(20)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS21 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(21)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS30 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(30)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS40 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(40)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS50 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(50)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS60 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(60)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS80 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(80)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();
        final FilterPattern passportUS90 = new FilterPattern.FilterPatternBuilder(Pattern.compile("\\b(90)[A-Z0-9]{4,8}\\b", Pattern.CASE_INSENSITIVE), 0.90).withClassification("US").build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("passport");

        this.analyzer = new Analyzer(contextualTerms,
                passportUS01,
                passportUS02,
                passportUS03,
                passportUS04,
                passportUS05,
                passportUS06,
                passportUS07,
                passportUS08,
                passportUS09,
                passportUS10,
                passportUS11,
                passportUS12,
                passportUS13,
                passportUS14,
                passportUS15,
                passportUS16,
                passportUS17,
                passportUS20,
                passportUS21,
                passportUS30,
                passportUS40,
                passportUS50,
                passportUS60,
                passportUS80,
                passportUS90);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, spans);

    }

}