package io.philterd.phileas.model.responses;

import java.util.List;

public class DetectResponse {

    private List<String> types;

    public DetectResponse(List<String> types) {
        this.types = types;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

}
