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
package ai.philterd.phileas.services.split;

import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Creates the {@link SplitService} for a configured splitting method. */
public class SplitFactory {

    private static final Logger LOGGER = LogManager.getLogger(SplitFactory.class);

    /**
     * Returns the split service for a splitting method: {@code newline}, {@code width}, or
     * {@code characters}, ignoring case. {@code character} is accepted as an alias for
     * {@code characters}, since the redaction policy specification's splitting example uses it.
     * @param method The splitting method named by the policy.
     * @param threshold The piece size used by the {@code width} and {@code characters} methods.
     * @return The {@link SplitService} for the method.
     * @throws IllegalArgumentException Thrown if the method is not one of the accepted names.
     */
    public static SplitService getSplitService(final String method, final int threshold) {

        if(Strings.CI.equals("newline", method)) {

            LOGGER.debug("Instantiating a newline split service.");
            return new NewLineSplitService();

        } else if(Strings.CI.equals("width", method)) {

            // TODO: Make line width configurable.
            LOGGER.debug("Instantiating a line width split service.");
            return new LineWidthSplitService(threshold);

        } else if(Strings.CI.equals("characters", method) || Strings.CI.equals("character", method)) {

            LOGGER.debug("Instantiating a character count split service.");
            return new CharacterCountSplitService(threshold);

        }

        // Splitting by a method the policy did not ask for gives pieces, and overlap boundaries,
        // the policy author never intended.
        throw new IllegalArgumentException("Unsupported split method '" + method
                + "'. This build accepts: newline, width, characters, character.");

    }

}
