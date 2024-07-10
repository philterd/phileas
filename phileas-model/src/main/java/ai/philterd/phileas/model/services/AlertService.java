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
package ai.philterd.phileas.model.services;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Alert;

import java.util.List;

public interface AlertService {

    /**
     * Generate an alert.
     *
     * @param policy The name of the policy.
     * @param strategyId The ID of the filter strategy that caused the alert.
     * @param context The context.
     * @param documentId The document ID.
     * @param filterType The {@link FilterType}.
     */
    void generateAlert(String policy, String strategyId, String context, String documentId, FilterType filterType);

    /**
     * Gets all alerts.
     * @return A list of all the alerts.
     */
    List<Alert> getAlerts();

    /**
     * Delete an alert.
     * @param alertId The ID of the alert.
     */
    void delete(String alertId);

    /**
     * Remove all alerts.
     */
    void clear();

}
