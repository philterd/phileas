package com.mtnfog.test.phileas.model.responses;

import com.mtnfog.phileas.model.responses.FilterResponse;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class FilterResponseTest {

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(FilterResponse.class).verify();
    }

}
