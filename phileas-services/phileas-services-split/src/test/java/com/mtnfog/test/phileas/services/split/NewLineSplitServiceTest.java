package com.mtnfog.test.phileas.services.split;

import com.mtnfog.phileas.model.services.SplitService;
import com.mtnfog.phileas.services.split.NewLineSplitService;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NewLineSplitServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(NewLineSplitServiceTest.class);

    @Test
    public void split0() throws IOException {

        final File file = new File("src/test/resources/simple-test.txt");
        final String input = FileUtils.readFileToString(file, Charset.defaultCharset());

        assertTrue(input != null);
        LOGGER.info("Input text length = " + input.length());

        final SplitService splitService = new NewLineSplitService();
        final List<String> splits = splitService.split(input);

        for(final String split : splits) {
            LOGGER.info(split);
        }

    }

    @Test
    public void split1() throws IOException {

        final File file = new File("src/test/resources/alice29.txt");
        final String input = FileUtils.readFileToString(file, Charset.defaultCharset());

        assertTrue(input != null);
        LOGGER.info("Input text length = " + input.length());

        final SplitService splitService = new NewLineSplitService();
        final List<String> splits = splitService.split(input);

        LOGGER.info("Splits = " + splits.size());
        assertEquals(2732, splits.size());

        for(final String split : splits) {
            LOGGER.info(split);
        }

    }

    @Test
    public void split2() throws IOException {

        final File file = new File("src/test/resources/alice29-formatted.txt");
        String input = FileUtils.readFileToString(file, Charset.defaultCharset());

        assertTrue(input != null);
        LOGGER.info("Input text length = " + input.length());

        final SplitService splitService = new NewLineSplitService();
        final List<String> splits = splitService.split(input);

        LOGGER.info("Splits = " + splits.size());
        assertEquals(6, splits.size());

        for(final String split : splits) {
            LOGGER.info(split);
        }

    }

}
