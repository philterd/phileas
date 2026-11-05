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

/**
 * Thrown when a policy-supplied regular expression does not compile, or fails a probe run when the
 * policy is loaded. The policy is rejected rather than failing on whichever document first reaches
 * the pattern.
 */
public class InvalidPolicyPatternException extends RuntimeException {

    public InvalidPolicyPatternException(final String message) {
        super(message);
    }

    public InvalidPolicyPatternException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
