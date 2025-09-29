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
package ai.philterd.phileas.model.enums;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A sensitivity level.
 */
public enum SensitivityLevel {

    AUTO("auto"), OFF("off"), LOW("low"), MEDIUM("medium"), HIGH("high");

    private final String name;

    private SensitivityLevel(String type) {
        this.name = type;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Gets a {@link SensitivityLevel} from a name.
     * @param name The name.
     * @return A corresponding {@link SensitivityLevel}.
     */
    public static SensitivityLevel fromName(String name) {

        final Logger logger = LogManager.getLogger(SensitivityLevel.class);

        if(StringUtils.equalsIgnoreCase(name, AUTO.getName())) {
            return AUTO;
        } else if(StringUtils.equalsIgnoreCase(name, OFF.getName())) {
            return OFF;
        } else if(StringUtils.equalsIgnoreCase(name, LOW.getName())) {
            return LOW;
        } else if(StringUtils.equalsIgnoreCase(name, MEDIUM.getName())) {
            return MEDIUM;
        } if(StringUtils.equalsIgnoreCase(name, HIGH.getName())) {
            return HIGH;
        } else {
            logger.warn("Invalid sensitivity level. Valid are off, low, medium, high. Defaulting to high.");
            return HIGH;
        }

    }

}
