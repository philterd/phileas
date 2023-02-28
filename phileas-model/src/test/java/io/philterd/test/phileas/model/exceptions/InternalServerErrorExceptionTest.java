package io.philterd.test.phileas.model.exceptions;

import io.philterd.phileas.model.exceptions.api.InternalServerErrorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InternalServerErrorExceptionTest {

    @Test
    public void test() {

        final String message = "This is a test exceptinon.";

        InternalServerErrorException ex = new InternalServerErrorException(message);
        Assertions.assertEquals(message, ex.getMessage());

    }

}
