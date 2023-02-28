package io.philterd.test.phileas.services.alerts;

import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.objects.Alert;
import io.philterd.phileas.model.services.AlertService;
import io.philterd.phileas.services.alerts.LocalAlertService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LocalAlertServiceTest {

    @Test
    public void generate1() {

        final AlertService alertService = new LocalAlertService();

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.PERSON);

        final List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(1, alerts.size());

    }

    @Test
    public void delete1() {

        final AlertService alertService = new LocalAlertService();

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.PERSON);
        alertService.generateAlert("fp", "id", "context", "docid", FilterType.PERSON);
        alertService.generateAlert("fp", "id", "context", "docid", FilterType.PERSON);

        List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(3, alerts.size());

        alertService.delete(alerts.get(0).getId());

        alerts = alertService.getAlerts();

        Assertions.assertEquals(2, alerts.size());

    }

    @Test
    public void clear1() {

        final AlertService alertService = new LocalAlertService();

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.PERSON);

        List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(1, alerts.size());

        alertService.clear();

        alerts = alertService.getAlerts();

        Assertions.assertEquals(0, alerts.size());

    }

}
