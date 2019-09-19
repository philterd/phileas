package com.mtnfog.test.phileas.model.api;

import com.mtnfog.phileas.model.api.Status;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class StatusTest {

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(Status.class).verify();
    }

}
