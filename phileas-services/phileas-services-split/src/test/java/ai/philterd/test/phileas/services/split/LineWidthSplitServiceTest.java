/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.services.split.LineWidthSplitService;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LineWidthSplitServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(LineWidthSplitServiceTest.class);

    @Test
    public void split0() throws IOException {

        final File file = new File("src/test/resources/simple-test.txt");
        final String input = FileUtils.readFileToString(file, Charset.defaultCharset());

        assertTrue(input != null);
        LOGGER.info("Input text length = " + input.length());

        final SplitService splitService = new LineWidthSplitService(500);
        final List<String> splits = splitService.split(input);

        for(final String split : splits) {
            LOGGER.info(split);
        }

        Assertions.assertTrue(splits.contains("Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do:  once or twice she had peeped into the book her sister was reading, but it had no pictures or conversations in it, `and what is the use of a book,' thought Alice `without pictures or conversation?'"));
        Assertions.assertTrue(splits.contains("So she was considering in her own mind (as well as she could, for the hot day made her feel very sleepy and stupid), whether the pleasure of making a daisy-chain would be worth the trouble of"));
        Assertions.assertTrue(splits.contains("getting up and picking the daisies, when suddenly a White Rabbit with pink eyes ran close by her."));

    }

}
