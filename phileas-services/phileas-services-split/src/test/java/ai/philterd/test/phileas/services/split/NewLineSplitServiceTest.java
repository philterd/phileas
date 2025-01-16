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
package ai.philterd.test.phileas.services.split;

import ai.philterd.phileas.model.services.SplitService;
import ai.philterd.phileas.services.split.NewLineSplitService;
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
