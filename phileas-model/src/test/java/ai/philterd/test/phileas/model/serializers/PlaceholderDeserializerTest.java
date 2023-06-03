package ai.philterd.test.phileas.model.serializers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ai.philterd.phileas.model.serializers.PlaceholderDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlaceholderDeserializerTest {

    private Gson gson;

    @BeforeEach
    public void beforeEach() {

        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(String.class, new PlaceholderDeserializer());
        gson = gsonBuilder.create();

    }

    @Test
    public void test1() {

        // TODO: PHL-233: Write tests.

    }

}
