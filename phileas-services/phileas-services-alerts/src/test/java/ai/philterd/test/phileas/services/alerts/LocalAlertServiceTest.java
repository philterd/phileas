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
package ai.philterd.test.phileas.services.alerts;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Alert;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.alerts.LocalAlertService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LocalAlertServiceTest {

    @Test
    public void generate1() {

        final AlertService alertService = new LocalAlertService();

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.AGE);

        final List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(1, alerts.size());

    }

    @Test
    public void delete1() {

        final AlertService alertService = new LocalAlertService();

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.AGE);
        alertService.generateAlert("fp", "id", "context", "docid", FilterType.AGE);
        alertService.generateAlert("fp", "id", "context", "docid", FilterType.AGE);

        List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(3, alerts.size());

        alertService.delete(alerts.get(0).getId());

        alerts = alertService.getAlerts();

        Assertions.assertEquals(2, alerts.size());

    }

    @Test
    public void clear1() {

        final AlertService alertService = new LocalAlertService();

        alertService.generateAlert("fp", "id", "context", "docid", FilterType.AGE);

        List<Alert> alerts = alertService.getAlerts();

        Assertions.assertEquals(1, alerts.size());

        alertService.clear();

        alerts = alertService.getAlerts();

        Assertions.assertEquals(0, alerts.size());

    }

}
