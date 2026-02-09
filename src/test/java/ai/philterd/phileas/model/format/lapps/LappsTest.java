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
package ai.philterd.phileas.model.format.lapps;

import ai.philterd.phileas.model.formats.lapps.Annotation;
import ai.philterd.phileas.model.formats.lapps.Lapps;
import ai.philterd.phileas.model.formats.lapps.View;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class LappsTest {

    @Test
    public void lapps1() throws IOException {

        final File file = new File("src/test/resources/lapps/lapps1.json");
        final String input = FileUtils.readFileToString(file, Charset.defaultCharset());

        final Gson gson = new Gson();
        final Lapps lapps = gson.fromJson(input, Lapps.class);

        Assertions.assertEquals(1, lapps.getViews().size());

        for(final View view : lapps.getViews()) {

            Assertions.assertEquals(165, view.getAnnotations().size());

            for(final Annotation annotation : view.getAnnotations()) {

                if(Strings.CI.equals(Lapps.NAMED_ENTITY, annotation.getType())) {

                    if(annotation.getFeatures() != null) {

                        if(Strings.CI.equals("PER", annotation.getFeatures().getCategory())) {

                            Assertions.assertEquals(282, annotation.getStart());
                            Assertions.assertEquals(295, annotation.getEnd());
                            Assertions.assertEquals("PER", annotation.getFeatures().getCategory());
                            Assertions.assertEquals("James Smith's", lapps.getText().getValue().substring(annotation.getStart(), annotation.getEnd()));

                        }

                        if(Strings.CI.equals("PHONE_NUMBER", annotation.getFeatures().getCategory())) {

                            Assertions.assertEquals(300, annotation.getStart());
                            Assertions.assertEquals(315, annotation.getEnd());
                            Assertions.assertEquals("PHONE_NUMBER", annotation.getFeatures().getCategory());

                        }

                    }

                }

            }

        }

    }

}
