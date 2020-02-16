package com.mtnfog.test.phileas.services.processors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Structured;
import com.mtnfog.phileas.model.profile.fhir4.FhirItem;
import com.mtnfog.phileas.model.profile.fhir4.FhirR4;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.services.MetricsService;
import com.mtnfog.phileas.services.processors.FhirV4DocumentProcessor;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.Charset;
import java.util.Arrays;

public class FhirV4DocumentProcessorTest {

    @Test
    public void test1() throws Exception {

        final MetricsService metricsService = Mockito.mock(MetricsService.class);

        final FhirV4DocumentProcessor documentProcessor = new FhirV4DocumentProcessor(metricsService);

        // FilterProfile filterProfile, String context, String documentId, String json
        final FilterProfile filterProfile = new FilterProfile();
        filterProfile.setCrypto(new Crypto("key", "iv"));

        final Structured structured = new Structured();

        final FhirR4 fhirR4 = new FhirR4();
        fhirR4.setFhirItems(Arrays.asList(new FhirItem("address.city", "DELETE")));
        structured.setFhirR4(fhirR4);

        filterProfile.setStructured(structured);

        prettyPrintJson(filterProfile);

        final String json = IOUtils.toString(this.getClass().getResourceAsStream("/fhir4/bundle-example.json"), Charset.defaultCharset());

        final FilterResponse filterResponse = documentProcessor.process(filterProfile, "context", "documentId", json);

        prettyPrintJson(filterResponse.getFilteredText());

    }

    private void prettyPrintJson(Object object) {

        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(object));
        
    }

    private void prettyPrintJson(String uglyJSONString) {

        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final JsonParser jp = new JsonParser();
        final JsonElement je = jp.parse(uglyJSONString);
        System.out.println(gson.toJson(je));

    }

}
