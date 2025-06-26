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
package ai.philterd.phileas.model.services;

import ai.philterd.phileas.model.enums.FilterType;

/**
 * The metrics service is intended to track _only_ things that an implementer
 * of the Phileas library does _not_ have access to. Things like filter type counts
 * can be tracked by the implementer so they should not be captured by this service.
 */
public interface MetricsService {

    /**
     * The elapsed time for each filter.
     * @param filterType A {@link FilterType filterType}.
     * @param timeMs The filter execution time in milliseconds.
     */
    void logFilterTime(FilterType filterType, long timeMs);

}
