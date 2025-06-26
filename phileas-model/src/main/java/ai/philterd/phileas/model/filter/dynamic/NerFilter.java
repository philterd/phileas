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
package ai.philterd.phileas.model.filter.dynamic;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.services.MetricsService;

import java.util.Map;

/**
 * A dynamic filter that operates using named-entity recognition.
 */
public abstract class NerFilter extends DynamicFilter {

    protected String type;
    protected MetricsService metricsService;
    protected Map<String, Double> thresholds;

    /**
     * Creates a new filter.
     *
     * @param filterConfiguration The {@link FilterConfiguration} for the filter.
     * @param metricsService The {@link MetricsService}.
     * @param thresholds
     * @param filterType
     */
    public NerFilter(final FilterConfiguration filterConfiguration,
                     final MetricsService metricsService,
                     final Map<String, Double> thresholds,
                     final FilterType filterType) {

        super(filterType, filterConfiguration);

        this.metricsService = metricsService;
        this.thresholds = thresholds;

    }

}
