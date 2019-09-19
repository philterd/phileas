package com.mtnfog.test.phileas.model.exceptions;

import com.mtnfog.phileas.model.exceptions.api.UnauthorizedException;
import org.junit.Assert;
import org.junit.Test;

public class UnauthorizedExceptionTest {

    @Test
    public void test() {

        final String message = "This is a test exceptinon.";

        UnauthorizedException ex = new UnauthorizedException(message);
        Assert.assertEquals(message, ex.getMessage());

    }

}
