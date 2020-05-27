package com.mtnfog.phileas.services.alerts;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Alert;
import com.mtnfog.phileas.model.services.AlertService;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LocalAlertService implements AlertService {

    private List<Alert> alerts;

    public LocalAlertService() {
        this.alerts = new LinkedList<>();
    }

    @Override
    public void generateAlert(String filterProfile, String strategyId, String context, String documentId, FilterType filterType) {

        final Alert alert = new Alert(filterProfile, strategyId, context, documentId, filterType.getType());

        alerts.add(alert);

    }

    @Override
    public List<Alert> getAlerts() {

        return alerts;

    }

    @Override
    public void remove(String alertId) {

        alerts.removeIf(obj -> obj.getId() == alertId);

    }

    @Override
    public void clear() {

        alerts.clear();

    }

}
