/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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

/**
 * Allows for modifying the confidence of a span based on a given condition. The confidence can be either
 * overridden with a constant value, or can be modified by a delta value.
 */
public class ConfidenceModifier {

    private double confidence;
    private double confidenceDelta;
    private final ConfidenceCondition confidenceCondition;
    private final String characters;

    /**
     * Modifies the confidence of a span.
     * @param confidence The value to replace the span's confidence with.
     * @param confidenceCondition The condition that must be met.
     * @param characters The characters for the condition.
     */
    public ConfidenceModifier(final double confidence, final ConfidenceCondition confidenceCondition, final String characters) {
        this.confidence = confidence;
        this.confidenceCondition = confidenceCondition;
        this.characters = characters;
    }

    /**
     * Modifies the confidence of a span.
     * @param confidenceCondition The condition that must be met.
     * @param confidenceDelta The span's confidence value will be summed with this value. Use a negative delta to reduce the confidence.
     * @param characters The characters for the condition.
     */
    public ConfidenceModifier(final ConfidenceCondition confidenceCondition, final double confidenceDelta, final String characters) {
        this.confidenceCondition = confidenceCondition;
        this.confidenceDelta = confidenceDelta;
        this.characters = characters;
    }

    public double getConfidence() {
        return confidence;
    }

    public double getConfidenceDelta() {
        return confidenceDelta;
    }

    public String getCharacters() {
        return characters;
    }

    public ConfidenceCondition getConfidenceCondition() {
        return confidenceCondition;
    }

    public enum ConfidenceCondition {

        CHARACTER_SEQUENCE_BEFORE,
        CHARACTER_SEQUENCE_AFTER,
        CHARACTER_SEQUENCE_SURROUNDING;

    }

}
