package com.mtnfog.phileas.services.split;

import com.mtnfog.phileas.model.services.SplitService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class NewLineSplitService extends AbstractSplitService implements SplitService {

    private static final Logger LOGGER = LogManager.getLogger(NewLineSplitService.class);

    @Override
    public List<String> split(String input) {

        // Simply splits the input based on a number of new line operators.
        // See https://stackoverflow.com/a/31060125/1428388.
        return Arrays.asList(input.split("\\R"));

    }

}
