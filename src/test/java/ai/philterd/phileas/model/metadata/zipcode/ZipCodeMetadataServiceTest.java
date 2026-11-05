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

    @Test
    public void getPrefixPopulation() throws IOException {

        final ZipCodeMetadataService zipCodeMetadataService = new ZipCodeMetadataService();

        Assertions.assertEquals(13153, zipCodeMetadataService.getPopulationForPrefix("036"));
        Assertions.assertEquals(20310, zipCodeMetadataService.getPopulationForPrefix("576"));
        Assertions.assertEquals(1245296, zipCodeMetadataService.getPopulationForPrefix("902"));
        Assertions.assertEquals(0, zipCodeMetadataService.getPopulationForPrefix("202"));
        Assertions.assertEquals(0, zipCodeMetadataService.getPopulationForPrefix("000"));
        Assertions.assertEquals(0, zipCodeMetadataService.getPopulationForPrefix("90"));
        Assertions.assertEquals(0, zipCodeMetadataService.getPopulationForPrefix(null));

    }

}
