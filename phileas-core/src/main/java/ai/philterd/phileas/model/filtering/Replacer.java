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
package ai.philterd.phileas.model.filtering;

/**
 * Makes a span's replacement for a token, as the filter that found the span would. Used when a span's
 * text is changed after it was found, so its replacement can be made from the text it now covers.
 */
@FunctionalInterface
public interface Replacer {

    /**
     * @param token The text to replace.
     * @return The replacement for the token.
     * @throws Exception Thrown if the replacement cannot be made.
     */
    Replacement replace(String token) throws Exception;

}
