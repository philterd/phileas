package com.mtnfog.test.phileas.model.exceptions;

import com.mtnfog.phileas.model.exceptions.api.InternalServerErrorException;
import org.junit.Assert;
import org.junit.Test;

public class InternalServerErrorExceptionTest {

    @Test
    public void test() {

        final String message = "This is a test exceptinon.";

        InternalServerErrorException ex = new InternalServerErrorException(message);
        Assert.assertEquals(message, ex.getMessage());

    }

}
