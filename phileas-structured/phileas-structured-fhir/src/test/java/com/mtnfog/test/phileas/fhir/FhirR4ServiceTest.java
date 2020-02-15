package com.mtnfog.test.phileas.fhir;

import com.mtnfog.phileas.fhir.FhirR4Service;
import com.mtnfog.phileas.model.services.FhirService;
import org.junit.Test;

public class FhirR4ServiceTest {

    @Test
    public void process1() {

        final String json = "";

        final FhirService fhirService = new FhirR4Service();
        fhirService.process(json);

    }

}
