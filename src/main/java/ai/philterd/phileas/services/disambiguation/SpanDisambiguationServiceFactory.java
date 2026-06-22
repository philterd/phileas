/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.disambiguation;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.services.disambiguation.vector.VectorBasedSpanDisambiguationService;

/**
 * Builds the {@link SpanDisambiguationService} appropriate for the configuration: the real
 * vector-based implementation when span disambiguation is enabled, or a {@link NoOpSpanDisambiguationService}
 * when it is disabled. Letting construction decide which implementation is used removes the need for
 * callers to check whether the feature is enabled before invoking it.
 */
public final class SpanDisambiguationServiceFactory {

    private SpanDisambiguationServiceFactory() {
        // Utility class.
    }

    /**
     * @param phileasConfiguration The configuration that decides whether disambiguation is enabled.
     * @return The vector-based service if enabled, otherwise a no-op service.
     */
    public static SpanDisambiguationService getSpanDisambiguationService(
            final PhileasConfiguration phileasConfiguration) {

        if(phileasConfiguration.spanDisambiguationEnabled()) {
            return new VectorBasedSpanDisambiguationService(phileasConfiguration);
        }

        return new NoOpSpanDisambiguationService();

    }

}
