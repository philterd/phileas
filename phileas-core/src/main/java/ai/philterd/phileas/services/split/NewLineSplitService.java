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
package ai.philterd.phileas.services.split;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class NewLineSplitService extends AbstractSplitService implements SplitService {

    private static final Logger LOGGER = LogManager.getLogger(NewLineSplitService.class);

    private static final String SEPARATOR = System.lineSeparator();

    @Override
    public List<String> split(final String input) {

        // This method is faster than the \R regex.
        // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#lines()
        // This method provides better performance than split("\R") by supplying elements lazily and by faster search of new line terminators.
        final List<String> splits = Arrays.asList(input.lines().toArray(String[]::new));

        LOGGER.debug("Split large input exceeding threshold into {} splits using new line split method.", splits.size());

        return clean(splits);

    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

}
