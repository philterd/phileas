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
package ai.philterd.phileas.services.metrics;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.services.MetricsService;

/**
 * An implementation of {@link MetricsService} that does nothing with the metrics.
 */
public class NoOpMetricsService implements MetricsService {

    @Override
    public void incrementProcessed() {

    }

    @Override
    public void incrementProcessed(long count) {

    }

    @Override
    public void incrementFilterType(FilterType filterType) {

    }

    @Override
    public void logFilterTime(FilterType filterType, long timeMs) {

    }

}
