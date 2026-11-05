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
package ai.philterd.phileas.services.filters.ai.pheye;

import java.io.IOException;

/**
 * Thrown when a PhEye filter cannot get detections from its PhEye service: the service could not be reached,
 * or it answered with an error. Nothing is redacted by the filter for that text, so the document should not be
 * treated as filtered. {@link #getEndpoint()} names the service, for an operator; it is not part of the message,
 * which a caller may pass on to an end user.
 */
public class PhEyeUnavailableException extends IOException {

    private final String endpoint;

    public PhEyeUnavailableException(final String message, final String endpoint, final Throwable cause) {
        super(message, cause);
        this.endpoint = endpoint;
    }

    /** The PhEye service the filter called. */
    public String getEndpoint() {
        return endpoint;
    }

}
