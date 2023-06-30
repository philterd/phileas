/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.objects;

import java.util.regex.Pattern;

public class FilterPattern {

    private Pattern pattern;
    private String format;
    private double initialConfidence;
    private String classification;
    private boolean alwaysValid;
    private int groupNumber = 0;

    public static class FilterPatternBuilder {

        private Pattern pattern;
        private double initialConfidence;
        private String format;
        private String classification;
        private boolean alwaysValid = false;
        private int groupNumber = 0;

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

        public FilterPatternBuilder withAlwaysValid(boolean alwaysValid) {
            this.alwaysValid = alwaysValid;
            return this;
        }

        public FilterPatternBuilder withGroupNumber(int groupNumber) {
            this.groupNumber = groupNumber;
            return this;
        }

        public FilterPattern build() {
            return new FilterPattern(pattern, initialConfidence, format, classification, alwaysValid, groupNumber);
        }

    }

    private FilterPattern(Pattern pattern, double initialConfidence, String format, String classification, boolean alwaysValid, int groupNumber) {

        this.pattern = pattern;
        this.initialConfidence = initialConfidence;
        this.format = format;
        this.classification = classification;
        this.alwaysValid = alwaysValid;
        this.groupNumber = groupNumber;

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

    public int getGroupNumber() { return groupNumber; }

}