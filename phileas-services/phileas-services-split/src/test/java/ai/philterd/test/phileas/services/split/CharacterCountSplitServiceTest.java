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
import ai.philterd.phileas.services.split.CharacterCountSplitService;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class CharacterCountSplitServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(CharacterCountSplitServiceTest.class);

    @Test
    public void split0() throws IOException {

        final int splitLength = 250;

        final File file = new File("src/test/resources/simple-test.txt");
        final String input = FileUtils.readFileToString(file, Charset.defaultCharset());

        Assertions.assertNotNull(input);
        LOGGER.info("Input text length = {}", input.length());

        final SplitService splitService = new CharacterCountSplitService(splitLength);
        final List<String> splits = splitService.split(input);

        for(final String split : splits) {
            LOGGER.info("{} - {}", split.length(), split);
            Assertions.assertTrue(split.length() <= splitLength);
        }

        final StringBuilder sb = new StringBuilder();
        for(final String split : splits) {
            sb.append(split).append(splitService.getSeparator());
        }

        Assertions.assertEquals(input.trim(), sb.toString().trim());

    }

}
