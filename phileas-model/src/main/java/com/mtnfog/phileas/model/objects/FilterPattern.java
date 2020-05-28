package com.mtnfog.phileas.model.objects;

import java.util.regex.Pattern;

public class FilterPattern {

    private Pattern pattern;
    private String format;
    private double initialConfidence;
    private String classification;

    public static class FilterPatternBuilder {

        private Pattern pattern;
        private double initialConfidence;
        private String format;
        private String classification;

        public FilterPatternBuilder(Pattern pattern, double initialConfidence) {
            this.pattern = pattern;
            this.initialConfidence = initialConfidence;
        }

        public FilterPatternBuilder withFormat(String format) {
            this.format = format;
            return this;
        }

        public FilterPatternBuilder withClassification(String classification) {
            this.classification = classification;
            return this;
        }

        public FilterPattern build() {
            return new FilterPattern(pattern, initialConfidence, format, classification);
        }

    }

    private FilterPattern(Pattern pattern, double initialConfidence, String format, String classification) {

        this.pattern = pattern;
        this.initialConfidence = initialConfidence;
        this.format = format;
        this.classification = classification;

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

    public String getClassification() {
        return classification;
    }

}