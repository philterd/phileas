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
package ai.philterd.phileas.services;

import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.Review;

import java.security.SecureRandom;
import java.util.function.DoubleSupplier;

/**
 * Selects filter requests for human spot review at the rate a policy's {@code config.review.samplingRate} sets.
 * Selection is per request and stateless. It has its own random source, so drawing never changes the values
 * drawn for anonymization and never changes the redacted output.
 */
public final class ReviewSampler {

    private static final SecureRandom RANDOM = new SecureRandom();

    private ReviewSampler() {
        // Access the members of this class through its static methods.
    }

    /**
     * Draws once for a request filtered with the policy. Call this once per request, not per chunk or page.
     * @param policy The {@link Policy} the request is filtered with.
     * @return <code>true</code> if the request is selected for review.
     * @throws IllegalArgumentException Thrown if the policy's sampling rate is not from 0.0 to 1.0.
     */
    public static boolean isSelected(final Policy policy) {
        return isSelected(policy, RANDOM::nextDouble);
    }

    static boolean isSelected(final Policy policy, final DoubleSupplier draw) {

        final Review review = policy.getConfig() == null ? null : policy.getConfig().getReview();

        if (review == null) {
            return false;
        }

        review.validate();

        final double rate = review.getSamplingRate();

        // Draws only when the outcome is not already decided. A draw from [0, 1) is always below 1.0.
        if (rate == 0.0) {
            return false;
        } else if (rate == 1.0) {
            return true;
        }

        return draw.getAsDouble() < rate;

    }

}
