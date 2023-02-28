package io.philterd.phileas.model.metadata;

public interface MetadataService<T extends MetadataRequest, U extends MetadataResponse> {

    U getMetadata(T request);

}
