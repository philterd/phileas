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
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReviewSamplerTest {

    private static Policy policy(final String samplingRate) {
        return new Gson().fromJson("{\"config\":{\"review\":{\"samplingRate\":" + samplingRate + "}}}", Policy.class);
    }

    @Test
    public void rateOfZeroNeverSelects() {
        final Policy policy = policy("0.0");
        for (int i = 0; i < 10_000; i++) {
            Assertions.assertFalse(ReviewSampler.isSelected(policy));
        }
    }

    @Test
    public void rateOfOneAlwaysSelects() {
        final Policy policy = policy("1.0");
        for (int i = 0; i < 10_000; i++) {
            Assertions.assertTrue(ReviewSampler.isSelected(policy));
        }
    }

    @Test
    public void noReviewSettingNeverSelects() {
        Assertions.assertFalse(ReviewSampler.isSelected(new Policy()));
        Assertions.assertFalse(ReviewSampler.isSelected(new Gson().fromJson("{\"config\":{\"review\":null}}", Policy.class)));
    }

    @Test
    public void intermediateRateIsAchievedOverManyCalls() {
        final Policy policy = policy("0.25");
        final int calls = 100_000;
        int selected = 0;
        for (int i = 0; i < calls; i++) {
            if (ReviewSampler.isSelected(policy)) {
                selected++;
            }
        }
        // The standard deviation of the observed rate is about 0.0014, so 0.01 is a wide margin.
        Assertions.assertEquals(0.25, (double) selected / calls, 0.01);
    }

    @Test
    public void selectedWhenTheDrawIsBelowTheRate() {
        final Policy policy = policy("0.10");
        Assertions.assertTrue(ReviewSampler.isSelected(policy, () -> 0.0));
        Assertions.assertTrue(ReviewSampler.isSelected(policy, () -> 0.0999));
        Assertions.assertFalse(ReviewSampler.isSelected(policy, () -> 0.10));
        Assertions.assertFalse(ReviewSampler.isSelected(policy, () -> 0.9999));
    }

    @Test
    public void rateOutsideZeroToOneIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ReviewSampler.isSelected(policy("-0.1")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ReviewSampler.isSelected(policy("1.01")));

        final Policy nan = new Policy();
        nan.getConfig().getReview().setSamplingRate(Double.NaN);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ReviewSampler.isSelected(nan));
    }

}
