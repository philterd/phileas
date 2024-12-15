package ai.philterd.test.phileas.model.metadata.zipcode;

import ai.philterd.phileas.model.metadata.zipcode.ZipCodeMetadataRequest;
import ai.philterd.phileas.model.metadata.zipcode.ZipCodeMetadataResponse;
import ai.philterd.phileas.model.metadata.zipcode.ZipCodeMetadataService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ZipCodeMetadataServiceTest {

    @Test
    public void getZipCodePopulation1() throws IOException {

        final ZipCodeMetadataService zipCodeMetadataService = new ZipCodeMetadataService();

        final ZipCodeMetadataRequest zipCodeMetadataRequest = new ZipCodeMetadataRequest("90210");

        final ZipCodeMetadataResponse zipCodeMetadataResponse = zipCodeMetadataService.getMetadata(zipCodeMetadataRequest);

        Assertions.assertEquals(21741, zipCodeMetadataResponse.getPopulation());

    }

    @Test
    public void getZipCodePopulation2() throws IOException {

        final ZipCodeMetadataService zipCodeMetadataService = new ZipCodeMetadataService();

        final ZipCodeMetadataRequest zipCodeMetadataRequest = new ZipCodeMetadataRequest("90095");

        final ZipCodeMetadataResponse zipCodeMetadataResponse = zipCodeMetadataService.getMetadata(zipCodeMetadataRequest);

        Assertions.assertEquals(3, zipCodeMetadataResponse.getPopulation());

    }

    @Test
    public void invalidZipCode() throws IOException {

        final ZipCodeMetadataService zipCodeMetadataService = new ZipCodeMetadataService();

        final ZipCodeMetadataRequest zipCodeMetadataRequest = new ZipCodeMetadataRequest("12345");

        final ZipCodeMetadataResponse zipCodeMetadataResponse = zipCodeMetadataService.getMetadata(zipCodeMetadataRequest);

        Assertions.assertEquals(-1, zipCodeMetadataResponse.getPopulation());

    }

}
