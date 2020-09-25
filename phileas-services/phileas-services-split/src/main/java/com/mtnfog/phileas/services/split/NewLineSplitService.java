package com.mtnfog.phileas.services.split;

import com.mtnfog.phileas.model.services.SplitService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class NewLineSplitService extends AbstractSplitService implements SplitService {

    private static final Logger LOGGER = LogManager.getLogger(NewLineSplitService.class);

    private static final String SEPARATOR = System.lineSeparator();

    @Override
    public List<String> split(String input) {

        // return Arrays.asList(input.split(SEPARATOR));

        // Simply splits the input based on a number of new line operators.
        // See https://stackoverflow.com/a/31060125/1428388
        // return Arrays.asList(input.split("\\R+", -1));

        // This method is faster than the \R regex.
        // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#lines()
        // This method provides better performance than split("\R") by supplying elements lazily and by faster search of new line terminators.
        final List<String> splits = Arrays.asList(input.lines().toArray(String[]::new));

        LOGGER.info("Split large input exceeding threshold into {} splits using new line split method.", splits.size());

        return splits;

    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

}
