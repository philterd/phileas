package com.mtnfog.phileas.processors.images;

import com.mtnfog.phileas.model.responses.ImageFilterResponse;
import com.mtnfog.phileas.model.services.ImageProcessor;
import nu.pattern.OpenCV;
import org.apache.commons.io.FileUtils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class OpenCVImageProcessor implements ImageProcessor {

    private final File frontalFaceFile;
    private final File profileFaceFile;

    public OpenCVImageProcessor() throws IOException {

        OpenCV.loadLocally();

        final InputStream is1 = getClass().getClassLoader().getResourceAsStream("haarcascade_frontalface_alt.xml");
        frontalFaceFile = File.createTempFile("haarcascade_frontalface_alt", ".xml");
        FileUtils.copyInputStreamToFile(is1, frontalFaceFile);
        is1.close();

        final InputStream is2 = getClass().getClassLoader().getResourceAsStream("haarcascade_profileface.xml");
        profileFaceFile = File.createTempFile("haarcascade_profileface", ".xml");
        FileUtils.copyInputStreamToFile(is2, profileFaceFile);
        is2.close();

    }

    @Override
    public ImageFilterResponse process(byte[] image) {

        Mat loadedImage = Imgcodecs.imdecode(new MatOfByte(image), Imgcodecs.IMREAD_COLOR);

        MatOfRect matOfRect = detect(loadedImage,frontalFaceFile.getAbsolutePath());
        MatOfRect matOfRect2 = detect(loadedImage,"/mtnfog/code/philter/phileas/phileas-processors/phileas-processors-images/src/main/resources/haarcascade_profileface.xml");

        Rect[] facesArray = matOfRect.toArray();
        Rect[] facesArray2 = matOfRect2.toArray();

        final int redactions = facesArray.length + facesArray2.length;

        for(Rect face : facesArray) {
            Imgproc.rectangle(loadedImage, face.tl(), face.br(), new Scalar(0, 0, 255), -1);
        }

        for(Rect face : facesArray2) {
            Imgproc.rectangle(loadedImage, face.tl(), face.br(), new Scalar(0, 0, 255), -1);
        }

        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", loadedImage, matOfByte);
        byte[] byteArray = matOfByte.toArray();

        return new ImageFilterResponse(byteArray, redactions);

    }

    private MatOfRect detect(Mat loadedImage, String model) {

        final MatOfRect facesDetected = new MatOfRect();

        final CascadeClassifier cascadeClassifier = new CascadeClassifier();
        final int minFaceSize = Math.round(loadedImage.rows() * 0.01f);

        cascadeClassifier.load(model);
        cascadeClassifier.detectMultiScale(loadedImage,
                facesDetected,
                1.05,
                3,
                Objdetect.CASCADE_SCALE_IMAGE,
                new Size(minFaceSize, minFaceSize),
                new Size()
        );

        return facesDetected;

    }

}
