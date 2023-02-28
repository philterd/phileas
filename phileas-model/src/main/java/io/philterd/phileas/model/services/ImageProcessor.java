package io.philterd.phileas.model.services;

import io.philterd.phileas.model.responses.ImageFilterResponse;

public interface ImageProcessor {

    ImageFilterResponse process(byte[] image);

}
