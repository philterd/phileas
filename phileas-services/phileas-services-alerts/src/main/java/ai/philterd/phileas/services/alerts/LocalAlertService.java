/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.services.alerts;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Alert;
import ai.philterd.phileas.model.services.AlertService;
import org.apache.commons.lang3.StringUtils;
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
    public void generateAlert(String policy, String strategyId, String context, String documentId, FilterType filterType) {

        final Alert alert = new Alert(policy, strategyId, context, documentId, filterType.getType());

        alerts.add(alert);

    }

    @Override
    public List<Alert> getAlerts() {

        return alerts;

    }

    @Override
    public void delete(String alertId) {

        LOGGER.info("Deleting alert {}", alertId);
        alerts.removeIf(alert -> StringUtils.equalsIgnoreCase(alert.getId(), alertId));

    }

    @Override
    public void clear() {

        alerts.clear();

    }

}
