package io.philterd.phileas.model.services;

import java.util.List;

public interface SentenceDetector {

    List<String> detect(String input);

}
