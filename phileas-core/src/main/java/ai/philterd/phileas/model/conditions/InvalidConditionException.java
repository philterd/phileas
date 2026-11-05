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
package ai.philterd.phileas.model.conditions;

/**
 * Thrown when a filter strategy condition cannot be parsed. The message gives the position of the
 * error and what was expected, but never the condition's text, since a condition can contain a
 * sensitive value (for example {@code token != "123-45-6789"}).
 */
public class InvalidConditionException extends IllegalArgumentException {

    private final int position;

    public InvalidConditionException(final int position, final String reason) {
        super("The condition is not valid at character " + (position + 1) + ": " + reason);
        this.position = position;
    }

    /**
     * The zero-based position of the error in the condition.
     * @return The position.
     */
    public int getPosition() {
        return position;
    }

}
