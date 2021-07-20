package com.mtnfog.test.phileas.processors.image;

import com.mtnfog.phileas.model.responses.ImageFilterResponse;
import com.mtnfog.phileas.processors.images.OpenCVImageProcessor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class OpenCVImageProcessorTest {

    //Mat loadedImage = loadImage("/home/jeff/Desktop/05-29-Small-crowd.jpg");
    //Mat loadedImage = loadImage("/home/jeff/Desktop/7f181d-20200405-social-distance-bdemakaska-02.jpg");
    //Mat loadedImage = loadImage("/home/jeff/Desktop/_118233584_gettyimages-1219181276.jpg");

    @Test
    public void process() throws IOException {

        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("05-29-Small-crowd.jpg");
        byte[] targetArray = IOUtils.toByteArray(inputStream);

        final OpenCVImageProcessor openCVImageProcessor = new OpenCVImageProcessor();
        final ImageFilterResponse imageFilterResponse = openCVImageProcessor.process(targetArray);

        File file = File.createTempFile("image", ".jpg");
        FileUtils.writeByteArrayToFile(file, imageFilterResponse.getImage());
        System.out.println(file.getAbsolutePath());

        Assertions.assertEquals(922002, targetArray.length);
        Assertions.assertEquals(7, imageFilterResponse.getRedactions());

    }

    @Test
    public void process2() throws IOException {

        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("4412ea71-8edf-4eab-912b-64bcc18aaa57.jpeg");
        byte[] targetArray = IOUtils.toByteArray(inputStream);

        final OpenCVImageProcessor openCVImageProcessor = new OpenCVImageProcessor();
        final ImageFilterResponse imageFilterResponse = openCVImageProcessor.process(targetArray);

        File file = File.createTempFile("image", ".jpg");
        FileUtils.writeByteArrayToFile(file, imageFilterResponse.getImage());
        System.out.println(file.getAbsolutePath());

        Assertions.assertEquals(63544, targetArray.length);
        Assertions.assertEquals(2, imageFilterResponse.getRedactions());

    }

}
