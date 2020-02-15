package com.mtnfog.test.phileas.services.processors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Structured;
import com.mtnfog.phileas.model.profile.fhir4.FhirItem;
import com.mtnfog.phileas.model.profile.fhir4.FhirR4;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.services.MetricsService;
import com.mtnfog.phileas.model.services.PostFilter;
import com.mtnfog.phileas.model.services.Store;
import com.mtnfog.phileas.services.processors.DocumentProcessor;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.Charset;
import java.util.*;

public class DocumentProcessorTest {

    @Test
    public void test1() throws Exception {

        final MetricsService metricsService = Mockito.mock(MetricsService.class);
        final Store store = Mockito.mock(Store.class);

        final Map<String, List<Filter>> filters = new HashMap<>();
        final List<PostFilter> postFilters = new LinkedList<>();

        final DocumentProcessor documentProcessor = new DocumentProcessor(filters, postFilters, metricsService, store);

        // FilterProfile filterProfile, String context, String documentId, String json
        final FilterProfile filterProfile = new FilterProfile();

        final Structured structured = new Structured();

        final FhirR4 fhirR4 = new FhirR4();
        fhirR4.setFhirItems(Arrays.asList(new FhirItem("address.city", "DELETE")));
        structured.setFhirR4(fhirR4);

        filterProfile.setStructured(structured);

        final String json = IOUtils.toString(this.getClass().getResourceAsStream("/fhir4/bundle-example.json"), Charset.defaultCharset());

        final FilterResponse filterResponse = documentProcessor.processApplicationFhirJson(filterProfile, "context", "documentId", json);

        prettyPrintJson(filterResponse.getFilteredText());

    }

    private void prettyPrintJson(String uglyJSONString) {

        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final JsonParser jp = new JsonParser();
        final JsonElement je = jp.parse(uglyJSONString);
        System.out.println(gson.toJson(je));

    }

}
