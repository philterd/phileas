package ai.philterd.test.phileas.model.objects;

import com.google.gson.Gson;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Alert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AlertTest {

    @Test
    public void alertJson1() {

        final Alert alert = new Alert("my-filter-profile", "my-strategy", "context", "documentid", FilterType.CREDIT_CARD.getType());

        final Gson gson = new Gson();
        final String json = gson.toJson(alert);

        Assertions.assertNotNull(json);

        System.out.println(json);

    }

}
