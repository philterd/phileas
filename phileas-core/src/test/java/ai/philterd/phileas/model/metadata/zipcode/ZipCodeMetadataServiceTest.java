package ai.philterd.phileas.model.metadata.zipcode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ZipCodeMetadataServiceTest {

    @Test
    public void getZipCodePopulation1() throws IOException {

        final ZipCodeMetadataService zipCodeMetadataService = new ZipCodeMetadataService();

        final ZipCodeMetadataRequest zipCodeMetadataRequest = new ZipCodeMetadataRequest("90210");

        final ZipCodeMetadataResult zipCodeMetadataResponse = zipCodeMetadataService.getMetadata(zipCodeMetadataRequest);

        Assertions.assertEquals(21134, zipCodeMetadataResponse.getPopulation());

    }

    @Test
    public void getZipCodePopulation2() throws IOException {

        final ZipCodeMetadataService zipCodeMetadataService = new ZipCodeMetadataService();

        final ZipCodeMetadataRequest zipCodeMetadataRequest = new ZipCodeMetadataRequest("90095");

        final ZipCodeMetadataResult zipCodeMetadataResponse = zipCodeMetadataService.getMetadata(zipCodeMetadataRequest);

        Assertions.assertEquals(1, zipCodeMetadataResponse.getPopulation());

    }

    @Test
    public void invalidZipCode() throws IOException {

        final ZipCodeMetadataService zipCodeMetadataService = new ZipCodeMetadataService();

        final ZipCodeMetadataRequest zipCodeMetadataRequest = new ZipCodeMetadataRequest("12345");

        final ZipCodeMetadataResult zipCodeMetadataResponse = zipCodeMetadataService.getMetadata(zipCodeMetadataRequest);

        Assertions.assertEquals(-1, zipCodeMetadataResponse.getPopulation());

    }

}
