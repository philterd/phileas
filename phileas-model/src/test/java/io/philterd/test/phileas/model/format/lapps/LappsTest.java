package io.philterd.test.phileas.model.format.lapps;

import com.google.gson.Gson;
import io.philterd.phileas.model.formats.lapps.Annotation;
import io.philterd.phileas.model.formats.lapps.Lapps;
import io.philterd.phileas.model.formats.lapps.View;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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

                if(StringUtils.equalsIgnoreCase(Lapps.NAMED_ENTITY, annotation.getType())) {

                    if(annotation.getFeatures() != null) {

                        if(StringUtils.equalsIgnoreCase("PER", annotation.getFeatures().getCategory())) {

                            Assertions.assertEquals(282, annotation.getStart());
                            Assertions.assertEquals(295, annotation.getEnd());
                            Assertions.assertEquals("PER", annotation.getFeatures().getCategory());
                            Assertions.assertEquals("James Smith's", lapps.getText().getValue().substring(annotation.getStart(), annotation.getEnd()));

                        }

                        if(StringUtils.equalsIgnoreCase("PHONE_NUMBER", annotation.getFeatures().getCategory())) {

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
