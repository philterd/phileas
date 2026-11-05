/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.policy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

public class IgnoredPatternTest {

    @Test
    public void aMatchIsBoundedByTheBudget() {

        // https://github.com/philterd/phileas/issues/356: "(.*a){16}b" backtracks catastrophically on a run of
        // a's with no b. Unbounded it takes far longer than the budget; bounded it gives up and does not match.
        final IgnoredPattern pattern = new IgnoredPattern("(.*a){16}b");
        final String token = "a".repeat(30);

        final long start = System.nanoTime();
        final boolean matched = Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), () -> pattern.matches(token, 200));
        final long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        Assertions.assertFalse(matched, "a match that runs out of time does not ignore the token");
        Assertions.assertTrue(elapsedMs < 2000, "returned after " + elapsedMs + " ms");

    }

    @Test
    public void anOrdinaryMatchIsUnaffectedByTheBudget() {
        final IgnoredPattern pattern = new IgnoredPattern("[A-Z]{2}[0-9]{4}");
        Assertions.assertTrue(pattern.matches("AB1234", 200));
        Assertions.assertFalse(pattern.matches("AB123", 200));
        Assertions.assertTrue(pattern.matches("AB1234", 0), "a budget of zero or less disables the bound");
        Assertions.assertTrue(pattern.matches("AB1234"), "the one-argument form uses the default budget");
    }

}
