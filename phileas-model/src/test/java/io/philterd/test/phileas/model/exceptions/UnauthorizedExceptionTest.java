package io.philterd.test.phileas.model.exceptions;

import io.philterd.phileas.model.exceptions.api.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UnauthorizedExceptionTest {

    @Test
    public void test() {

        final String message = "This is a test exceptinon.";

        UnauthorizedException ex = new UnauthorizedException(message);
        Assertions.assertEquals(message, ex.getMessage());

    }

}
