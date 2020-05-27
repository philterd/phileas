package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Alert;

import java.util.List;

public interface AlertService {

    /**
     * Generate an alert.
     *
     * @param filterProfile The name of the filter profile.
     * @param strategyId The ID of the filter strategy that caused the alert.
     * @param context The context.
     * @param documentId The document ID.
     * @param filterType The {@link FilterType}.
     */
    void generateAlert(String filterProfile, String strategyId, String context, String documentId, FilterType filterType);

    /**
     * Gets all of the alerts.
     * @return A list of all the alerts.
     */
    List<Alert> getAlerts();

    /**
     * Remove an alert from the list.
     * @param alertId The ID of the alert.
     */
    void remove(String alertId);

    /**
     * Remove all alerts.
     */
    void clear();

}
