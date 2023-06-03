package ai.philterd.test.phileas.model.exceptions;

import ai.philterd.phileas.model.exceptions.api.ServiceUnavailableException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServiceUnavailableExceptionTest {

    @Test
    public void test() {

        final String message = "This is a test exceptinon.";

        ServiceUnavailableException ex = new ServiceUnavailableException(message);
        Assertions.assertEquals(message, ex.getMessage());

    }

}
