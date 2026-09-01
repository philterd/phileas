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
 * Thrown when a regular expression exceeds its time budget or overflows the stack while matching.
 * Filtering the document fails rather than continuing without that pattern. See issue #357.
 */
public class RegexMatchFailedException extends RuntimeException {

    public RegexMatchFailedException(final String message) {
        super(message);
    }

}
