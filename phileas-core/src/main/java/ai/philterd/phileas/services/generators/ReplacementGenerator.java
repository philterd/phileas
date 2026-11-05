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
package ai.philterd.phileas.services.generators;

/**
 * Produces a replacement value for a detected token. Used by the {@code MAP_REPLACE} filter
 * strategy to generate a replacement for a value that is absent from its lookup table.
 */
public interface ReplacementGenerator {

    /**
     * Generates a replacement for a detected token.
     * @param token The detected value.
     * @param label The entity label (filter type) of the detected value.
     * @return The generated replacement value.
     * @throws Exception if the generator fails, times out, or returns invalid output. The caller is
     *         expected to apply the strategy's fallback in that case so the token is never left in the clear.
     */
    String generate(String token, String label) throws Exception;

}
