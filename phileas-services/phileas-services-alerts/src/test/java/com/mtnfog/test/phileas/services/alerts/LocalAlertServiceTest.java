package com.mtnfog.test.phileas.services.alerts;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Alert;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.services.alerts.LocalAlertService;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class LocalAlertServiceTest {

    @Test
    public void generate1() {

        final AlertService alertService = new LocalAlertService();

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.NER_ENTITY);

        final List<Alert> alerts = alertService.getAlerts();

        Assert.assertEquals(1, alerts.size());

    }

    @Test
    public void remove1() {

        final AlertService alertService = new LocalAlertService();

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.NER_ENTITY);

        List<Alert> alerts = alertService.getAlerts();

        Assert.assertEquals(1, alerts.size());

        alertService.delete(alerts.get(0).getId());

        alerts = alertService.getAlerts();

        Assert.assertEquals(0, alerts.size());

    }

    @Test
    public void clear1() {

        final AlertService alertService = new LocalAlertService();

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.NER_ENTITY);

        List<Alert> alerts = alertService.getAlerts();

        Assert.assertEquals(1, alerts.size());

        alertService.clear();

        alerts = alertService.getAlerts();

        Assert.assertEquals(0, alerts.size());

    }

}
