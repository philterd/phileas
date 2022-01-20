package com.mtnfog.test.phileas.model.responses;

import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.responses.FilterResponse;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FilterResponseTest {

    private static final Logger LOGGER = LogManager.getLogger(FilterResponseTest.class);

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(FilterResponse.class).verify();
    }

    private void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}
