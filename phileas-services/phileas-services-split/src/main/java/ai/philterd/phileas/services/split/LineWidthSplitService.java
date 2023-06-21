/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.services.SplitService;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class LineWidthSplitService extends AbstractSplitService implements SplitService {

    private static final Logger LOGGER = LogManager.getLogger(LineWidthSplitService.class);

    private static final String SEPARATOR = System.lineSeparator();

    final int lineWidth;

    public LineWidthSplitService(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    @Override
    public List<String> split(final String input) {

        final String wrapped = WordUtils.wrap(input, lineWidth);
        final List<String> lines = Arrays.asList(wrapped.lines().toArray(String[]::new));

        return clean(lines);

    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

}
