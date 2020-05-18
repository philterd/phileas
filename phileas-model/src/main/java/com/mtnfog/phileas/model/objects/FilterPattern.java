package com.mtnfog.phileas.model.objects;

import java.util.regex.Pattern;

public class FilterPattern {

    private Pattern pattern;
    private String format;
    private double initialConfidence;

    public FilterPattern(Pattern pattern, double initialConfidence) {

        this.pattern = pattern;
        this.initialConfidence = initialConfidence;

    }

    public FilterPattern(Pattern pattern, String format, double initialConfidence) {

        this.pattern = pattern;
        this.format = format;
        this.initialConfidence = initialConfidence;

    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getFormat() {
        return format;
    }

    public double getInitialConfidence() {
        return initialConfidence;
    }

}