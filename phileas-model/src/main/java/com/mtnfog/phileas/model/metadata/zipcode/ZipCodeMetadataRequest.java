package com.mtnfog.phileas.model.metadata.zipcode;

import com.mtnfog.phileas.model.metadata.MetadataRequest;

public class ZipCodeMetadataRequest extends MetadataRequest {

    private String zipCode;

    public ZipCodeMetadataRequest(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

}
