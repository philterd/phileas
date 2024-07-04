/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.cache.AbstractRedisCacheService;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Alert;
import ai.philterd.phileas.model.services.AlertService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RList;

import java.io.IOException;
import java.util.List;

public class RedisAlertService extends AbstractRedisCacheService implements AlertService {

    private static final Logger LOGGER = LogManager.getLogger(RedisAlertService.class);

    private static final String CACHE_LIST_NAME = "alert";

    public RedisAlertService(PhileasConfiguration phileasConfiguration) throws IOException {
        super(phileasConfiguration);
        LOGGER.info("Initializing Redis alert service.");
    }

    @Override
    public void generateAlert(String policy, String strategyId, String context, String documentId, FilterType filterType) {

        final Alert alert = new Alert(policy, strategyId, context, documentId, filterType.getType());

        redisson.getList(CACHE_LIST_NAME).add(alert);

    }

    @Override
    public List<Alert> getAlerts() {

        return redisson.getList(CACHE_LIST_NAME);

    }

    @Override
    public void delete(String alertId) {

        LOGGER.info("Deleting alert {}", alertId);

        final RList<Alert> alerts = redisson.getList(CACHE_LIST_NAME);

        int index = -1;

        for(int x = 0; x < alerts.size(); x++) {

            if(StringUtils.equalsIgnoreCase(alertId, alerts.get(x).getId())) {
                index = x;
                break;
            }

        }

        if(index != -1) {
            alerts.remove(index);
        }

    }

    @Override
    public void clear() {

        redisson.getKeys().delete(CACHE_LIST_NAME);

    }

}
