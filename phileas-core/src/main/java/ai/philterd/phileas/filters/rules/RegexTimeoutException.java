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
package ai.philterd.phileas.filters.rules;

/**
 * Thrown when a regex match exceeds its configured time budget. Used as a ReDoS guard so a
 * catastrophic-backtracking pattern aborts instead of hanging the matching thread.
 */
public class RegexTimeoutException extends RuntimeException {

    public RegexTimeoutException(final String message) {
        super(message);
    }

}
