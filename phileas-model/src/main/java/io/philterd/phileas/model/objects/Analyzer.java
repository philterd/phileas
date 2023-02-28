package io.philterd.phileas.model.objects;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Analyzer {

    private Set<String> contextualTerms;
    private List<FilterPattern> filterPatterns;

    public Analyzer(FilterPattern ... p) {

        this.filterPatterns = new LinkedList<>();

        for(final FilterPattern filterPattern : p) {
            filterPatterns.add(filterPattern);
        }

    }

    public Analyzer(Set<String> contextualTerms, FilterPattern ... p) {

        this.contextualTerms = contextualTerms;
        this.filterPatterns = new LinkedList<>();

        for(final FilterPattern filterPattern : p) {
            filterPatterns.add(filterPattern);
        }

    }

    public Analyzer(List<FilterPattern> filterPatterns) {

        this.filterPatterns = filterPatterns;

    }

    public Analyzer(Set<String> contextualTerms, List<FilterPattern> filterPatterns) {

        this.contextualTerms = contextualTerms;
        this.filterPatterns = filterPatterns;

    }

    public List<FilterPattern> getFilterPatterns() {
        return filterPatterns;
    }

    public Set<String> getContextualTerms() {
        return contextualTerms;
    }

}
