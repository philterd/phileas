package com.mtnfog.phileas.services.alerts;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Alert;
import com.mtnfog.phileas.model.services.AlertService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

public class LocalAlertService implements AlertService {

    private static final Logger LOGGER = LogManager.getLogger(LocalAlertService.class);

    private List<Alert> alerts;

    public LocalAlertService() {

        LOGGER.info("Initializing local alert service.");
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
