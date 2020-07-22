package com.mtnfog.phileas.model.services;

import java.util.List;

public interface SplitService {

    List<String> split(String input);

    String getSeparator();

}
