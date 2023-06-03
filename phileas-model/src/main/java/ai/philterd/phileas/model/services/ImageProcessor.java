package ai.philterd.phileas.model.services;

import ai.philterd.phileas.model.responses.ImageFilterResponse;

public interface ImageProcessor {

    ImageFilterResponse process(byte[] image);

}
