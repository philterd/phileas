package com.mtnfog.phileas.model.objects;

import java.util.List;
import java.util.regex.Pattern;

public class Analyzer {

    private List<Pattern> patterns;

    public Analyzer(Pattern ... p) {

        for(final Pattern pattern : p) {
            patterns.add(pattern);
        }

    }

    public Analyzer(List<Pattern> patterns) {

        this.patterns = patterns;

    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

}
