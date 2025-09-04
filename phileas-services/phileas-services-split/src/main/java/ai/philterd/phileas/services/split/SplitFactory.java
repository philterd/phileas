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

import ai.philterd.phileas.model.services.SplitService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SplitFactory {

    private static final Logger LOGGER = LogManager.getLogger(SplitFactory.class);

    public static SplitService getSplitService(final String method, final int threshold) {

        if(StringUtils.equalsIgnoreCase("newline", method)) {

            LOGGER.debug("Instantiating a newline split service.");
            return new NewLineSplitService();

        } else if(StringUtils.equalsIgnoreCase("width", method)) {

            // TODO: Make line width configurable.
            LOGGER.debug("Instantiating a line width split service.");
            return new LineWidthSplitService(threshold);

        } else if(StringUtils.equalsIgnoreCase("characters", method)) {

            LOGGER.debug("Instantiating a character count split service.");
            return new CharacterCountSplitService(threshold);

        }

        LOGGER.warn("No matching split service found for {}. Defaulting to newline split service.", method);
        return new NewLineSplitService();

    }

}
