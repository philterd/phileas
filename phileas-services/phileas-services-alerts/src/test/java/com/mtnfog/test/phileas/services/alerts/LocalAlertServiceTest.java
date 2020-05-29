package com.mtnfog.test.phileas.services.alerts;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Alert;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.alerts.LocalAlertService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LocalAlertServiceTest {

    @Test
    public void generate1() {

        final AlertService alertService = new LocalAlertService();

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.NER_ENTITY);

        final List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(1, alerts.size());

    }

    @Test
    public void delete1() {

        final AlertService alertService = new LocalAlertService();

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.NER_ENTITY);
        alertService.generateAlert("fp", "id", "context", "docid", FilterType.NER_ENTITY);
        alertService.generateAlert("fp", "id", "context", "docid", FilterType.NER_ENTITY);

        List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(3, alerts.size());

        alertService.delete(alerts.get(0).getId());

        alerts = alertService.getAlerts();

        Assertions.assertEquals(2, alerts.size());

    }

    @Test
    public void clear1() {

        final AlertService alertService = new LocalAlertService();

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.NER_ENTITY);

        List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(1, alerts.size());

        alertService.clear();

        alerts = alertService.getAlerts();

        Assertions.assertEquals(0, alerts.size());

    }

}
