package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.FilterConfiguration;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AgeFilter extends RegexFilter {

    public AgeFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.AGE, filterConfiguration);

        final Pattern agePattern1 = Pattern.compile("\\b[0-9.]+[\\s]*(year|years|yrs|yr|yo)(.?)(\\s)*(old)?\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age1 = new FilterPattern.FilterPatternBuilder(agePattern1, 0.90).build();

        final Pattern agePattern2 = Pattern.compile("\\b(age)(d)?(\\s)*[0-9.]+\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age2 = new FilterPattern.FilterPatternBuilder(agePattern2, 0.90).build();

        final Pattern agePattern3 = Pattern.compile("\\b[0-9.]+[-]*(year|years|yrs|yr|yo)(.?)(-)*(old)?\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age3 = new FilterPattern.FilterPatternBuilder(agePattern3, 0.90).build();

        // 61 y/o
        final Pattern agePattern4 = Pattern.compile("\\b([0-9]{1,3}) (y\\/o)\\b", Pattern.CASE_INSENSITIVE);
        final FilterPattern age4 = new FilterPattern.FilterPatternBuilder(agePattern4, 0.90).build();

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("age");
        this.contextualTerms.add("years");

        this.analyzer = new Analyzer(contextualTerms, age1, age2, age3, age4);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        return new FilterResult(context, documentId, nonOverlappingSpans);

    }

    @Override
    public List<Span> postFilter(List<Span> spans) {

        final List<Span> postFilteredSpans = new LinkedList<>();

        for(final Span span : spans) {

            final List<String> window = Arrays.asList(span.getWindow())
                    .stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            // Determine between a date and an age.
            // Does it contain 'age' or 'old' or 'yo'? If not, drop it.
            // TODO: Should this list be exposed to the user and customizable?
            if(window.contains("age")
                    || span.getText().contains("aged")
                    || span.getText().contains("old")
                    || span.getText().contains("y/o")
                    || span.getText().contains("yo")) {

                postFilteredSpans.add(span);

            }

        }

        return postFilteredSpans;

    }

}
