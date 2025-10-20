/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.filtering;

import java.util.List;
import java.util.regex.Pattern;

public class FilterPattern {

    private final Pattern pattern;
    private final String format;
    private final double initialConfidence;
    private final String classification;
    private final boolean alwaysValid;
    private final int groupNumber;
    private final List<ConfidenceModifier> confidenceModifiers;

    public static class FilterPatternBuilder {

        private final Pattern pattern;
        private final double initialConfidence;
        private String format;
        private String classification;
        private boolean alwaysValid = false;
        private int groupNumber = 0;
        private List<ConfidenceModifier> confidenceModifiers;

        public FilterPatternBuilder(final Pattern pattern, final double initialConfidence) {
            this.pattern = pattern;
            this.initialConfidence = initialConfidence;
        }

        public FilterPatternBuilder(final Pattern pattern, final double initialConfidence, List<ConfidenceModifier> confidenceModifiers) {
            this.pattern = pattern;
            this.initialConfidence = initialConfidence;
            this.confidenceModifiers = confidenceModifiers;
        }

        public FilterPatternBuilder withConfidenceModifiers(List<ConfidenceModifier> confidenceModifiers) {
            this.confidenceModifiers = confidenceModifiers;
            return this;
        }

        public FilterPatternBuilder withFormat(String format) {
            this.format = format;
            return this;
        }

        public FilterPatternBuilder withClassification(String classification) {
            this.classification = classification;
            return this;
        }

        public FilterPatternBuilder withAlwaysValid(boolean alwaysValid) {
            this.alwaysValid = alwaysValid;
            return this;
        }

        public FilterPatternBuilder withGroupNumber(int groupNumber) {
            this.groupNumber = groupNumber;
            return this;
        }

        public FilterPattern build() {
            return new FilterPattern(pattern, initialConfidence, format, classification, alwaysValid, groupNumber, confidenceModifiers);
        }

    }

    private FilterPattern(final Pattern pattern, final double initialConfidence, final String format,
                          final String classification, final boolean alwaysValid, final int groupNumber,
                          final List<ConfidenceModifier> confidenceModifiers) {

        this.pattern = pattern;
        this.initialConfidence = initialConfidence;
        this.format = format;
        this.classification = classification;
        this.alwaysValid = alwaysValid;
        this.groupNumber = groupNumber;
        this.confidenceModifiers = confidenceModifiers;

    }

    public boolean isAlwaysValid() {
        return alwaysValid;
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

    public int getGroupNumber() {
        return groupNumber;
    }

    public List<ConfidenceModifier> getConfidenceModifiers() {
        return confidenceModifiers;
    }

}