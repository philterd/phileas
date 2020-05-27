package com.mtnfog.test.phileas.model.objects;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Alert;
import org.junit.Test;

public class AlertTest {

    @Test
    public void alertJson1() {

        final Alert alert = new Alert("my-filter-profile", "my-strategy", "context", "documentId", FilterType.CREDIT_CARD.getType());

        final Gson gson = new Gson();
        final String json = gson.toJson(alert);

        System.out.println(json);

    }

}
