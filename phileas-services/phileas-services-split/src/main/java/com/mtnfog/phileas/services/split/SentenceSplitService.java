package com.mtnfog.phileas.services.split;

import com.mtnfog.phileas.model.services.SplitService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class SentenceSplitService extends AbstractSplitService implements SplitService {

    private static final Logger LOGGER = LogManager.getLogger(SentenceSplitService.class);

    @Override
    public List<String> split(String input) {
        return null;
    }

    @Override
    public String getSeparator() {
        return " ";
    }

}
