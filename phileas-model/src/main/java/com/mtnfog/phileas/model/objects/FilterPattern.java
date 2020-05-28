package com.mtnfog.phileas.model.objects;

import java.util.regex.Pattern;

public class FilterPattern {

    private Pattern pattern;
    private String format;
    private double initialConfidence;
    private String label;

    public static class FilterPatternBuilder {

        private Pattern pattern;
        private double initialConfidence;
        private String format;
        private String label;

        public FilterPatternBuilder(Pattern pattern, double initialConfidence) {
            this.pattern = pattern;
            this.initialConfidence = initialConfidence;
        }

        public FilterPatternBuilder withFormat(String format) {
            this.format = format;
            return this;
        }

        public FilterPatternBuilder withLabel(String label) {
            this.label = label;
            return this;
        }

        public FilterPattern build() {
            return new FilterPattern(pattern, initialConfidence, format, label);
        }

    }

    private FilterPattern(Pattern pattern, double initialConfidence, String format, String label) {

        this.pattern = pattern;
        this.initialConfidence = initialConfidence;
        this.format = format;
        this.label = label;

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

    public String getLabel() {
        return label;
    }

}