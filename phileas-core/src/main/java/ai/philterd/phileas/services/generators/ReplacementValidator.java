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
 * Re-scans a candidate replacement produced by a {@link ReplacementGenerator} to confirm the
 * generator did not reintroduce sensitive information. A {@code MAP_REPLACE} strategy rejects a
 * generated value that contains PII and applies its fallback strategy instead, so a generator can
 * never emit new sensitive information into the output.
 */
public interface ReplacementValidator {

    /**
     * Determines whether a candidate replacement contains detectable PII.
     * @param candidate The generated replacement value to re-scan.
     * @return <code>true</code> if the candidate contains detectable PII (and must be rejected);
     *         otherwise <code>false</code>.
     */
    boolean containsPii(String candidate);

}
