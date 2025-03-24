/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.model.services.CacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DefaultAlertService implements AlertService {

    private static final Logger LOGGER = LogManager.getLogger(DefaultAlertService.class);

    private final CacheService cacheService;

    public DefaultAlertService(final CacheService cacheService) {
        LOGGER.info("Initializing local alert service.");
        this.cacheService = cacheService;
    }

    @Override
    public void generateAlert(String policy, String strategyId, String context, String documentId, FilterType filterType) {
        cacheService.generateAlert(policy, strategyId, context, documentId, filterType);

    }

    @Override
    public List<Alert> getAlerts() {
        return cacheService.getAlerts();
    }

    @Override
    public void delete(String alertId) {
        cacheService.deleteAlert(alertId);
    }

    @Override
    public void clear() {
        cacheService.clearAlerts();
    }

}
