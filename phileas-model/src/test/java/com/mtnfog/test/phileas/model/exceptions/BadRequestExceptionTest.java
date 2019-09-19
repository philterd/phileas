package com.mtnfog.test.phileas.model.exceptions;

import com.mtnfog.phileas.model.exceptions.api.BadRequestException;
import org.junit.Assert;
import org.junit.Test;

public class BadRequestExceptionTest {

    @Test
    public void test() {

        final String message = "This is a test exceptinon.";

        BadRequestException ex = new BadRequestException(message);
        Assert.assertEquals(message, ex.getMessage());

    }

}
