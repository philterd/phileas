package com.mtnfog.test.phileas.model.exceptions;

import com.mtnfog.phileas.model.exceptions.api.ServiceUnavailableException;
import org.junit.Assert;
import org.junit.Test;

public class ServiceUnavailableExceptionTest {

    @Test
    public void test() {

        final String message = "This is a test exceptinon.";

        ServiceUnavailableException ex = new ServiceUnavailableException(message);
        Assert.assertEquals(message, ex.getMessage());

    }

}
