package com.mtnfog.phileas.model.objects;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Analyzer {

    private Set<String> contextualTerms;
    private List<FilterPattern> filterPatterns;

    public Analyzer(Set<String> contextualTerms, FilterPattern ... p) {

        this.contextualTerms = contextualTerms;
        this.filterPatterns = new LinkedList<>();

        for(final FilterPattern filterPattern : p) {
            filterPatterns.add(filterPattern);
        }

    }

    public Analyzer(Set<String> contextualTerms, List<FilterPattern> filterPatterns) {

        this.contextualTerms = contextualTerms;
        this.filterPatterns = filterPatterns;

    }

    public List<FilterPattern> getFilterPatterns() {
        return filterPatterns;
    }

}
