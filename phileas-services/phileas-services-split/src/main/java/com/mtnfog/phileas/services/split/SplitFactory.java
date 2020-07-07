package com.mtnfog.phileas.services.split;

import com.mtnfog.phileas.model.services.SplitService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SplitFactory {

    private static final Logger LOGGER = LogManager.getLogger(SplitFactory.class);

    public static SplitService getSplitService(String method) {

        if(StringUtils.equalsIgnoreCase("newline", method)) {

            LOGGER.debug("Instantiating a newline split service.");
            return new NewLineSplitService();

        } else if(StringUtils.equalsIgnoreCase("sentence", method)) {

            LOGGER.debug("Instantiating a sentence split service.");
            return new SentenceSplitService();

        }

        LOGGER.warn("No matching split service found for {}. Defaulting to newline split service.", method);
        return new NewLineSplitService();

    }

}
