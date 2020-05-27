package com.mtnfog.phileas.model.enums;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;

/**
 * A sensitivity level.
 */
public enum SensitivityLevel {

    AUTO("auto"), LOW("low"), MEDIUM("medium"), HIGH("high");

    private String name;

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
        } else if(StringUtils.equalsIgnoreCase(name, LOW.getName())) {
            return LOW;
        } else if(StringUtils.equalsIgnoreCase(name, MEDIUM.getName())) {
            return MEDIUM;
        } if(StringUtils.equalsIgnoreCase(name, HIGH.getName())) {
            return HIGH;
        } else {
            logger.warn("Invalid sensitivity level. Valid are low, medium, high. Defaulting to high.");
            return HIGH;
        }

    }

}
