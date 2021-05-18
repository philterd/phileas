package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.responses.ImageFilterResponse;

public interface ImageProcessor {

    ImageFilterResponse process(byte[] image);

}
