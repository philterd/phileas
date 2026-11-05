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
 * Thrown when a policy names a file Phileas may not read: one that does not exist, is not a regular file, is outside
 * the allowed directories when they are restricted, or is larger than the configured limit.
 */
public class InvalidPolicyFileException extends IllegalArgumentException {

    public InvalidPolicyFileException(final String message) {
        super(message);
    }

}
