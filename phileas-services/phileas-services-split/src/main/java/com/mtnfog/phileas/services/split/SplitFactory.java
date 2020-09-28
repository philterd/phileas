package com.mtnfog.phileas.services.split;

import com.mtnfog.phileas.model.services.SplitService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class SplitFactory {

    private static final Logger LOGGER = LogManager.getLogger(SplitFactory.class);

    public static SplitService getSplitService(String method) throws IOException {

        if(StringUtils.equalsIgnoreCase("newline", method)) {

            LOGGER.debug("Instantiating a newline split service.");
            return new NewLineSplitService();

        } else if(StringUtils.equalsIgnoreCase("width", method)) {

            // TODO: Set line width.
            LOGGER.debug("Instantiating a line width split service.");
            return new LineWidthSplitService(80);

        }

        LOGGER.warn("No matching split service found for {}. Defaulting to newline split service.", method);
        return new NewLineSplitService();

    }

}
