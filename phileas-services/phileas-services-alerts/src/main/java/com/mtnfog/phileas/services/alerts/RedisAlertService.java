package com.mtnfog.phileas.services.alerts;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.cache.AbstractRedisCacheService;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Alert;
import com.mtnfog.phileas.model.services.AlertService;
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
    }

    @Override
    public void generateAlert(String strategyId, String context, String documentId, FilterType filterType) {

        final Alert alert = new Alert(strategyId, context, documentId, filterType.getType());

        redisson.getList(CACHE_LIST_NAME).add(alert);

    }

    @Override
    public List<Alert> getAlerts() {

        return redisson.getList(CACHE_LIST_NAME);

    }

    @Override
    public void remove(String alertId) {

        // TODO: Can this be improved so we don't have to enumerate over
        // all of the alerts?
        final RList<Alert> alerts = redisson.getList(CACHE_LIST_NAME);

        for(final Alert alert : alerts) {

            if(StringUtils.equalsIgnoreCase(alert.getId(), alertId)) {
                alerts.remove(alert);
            }

        }

    }

    @Override
    public void clear() {

        redisson.getKeys().delete(CACHE_LIST_NAME);

    }

}
