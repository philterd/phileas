package com.mtnfog.test.phileas.model.exceptions;

import com.mtnfog.phileas.model.exceptions.api.BadRequestException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BadRequestExceptionTest {

    @Test
    public void test() {

        final String message = "This is a test exceptinon.";

        BadRequestException ex = new BadRequestException(message);
        Assertions.assertEquals(message, ex.getMessage());

    }

}
