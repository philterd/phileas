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

/**
 * Thrown when a PhEye filter is configured for local inference (a {@code modelPath} is set) but no
 * {@link PhEyeDetectorProvider} is on the classpath to run it. Local inference is supplied by the
 * optional {@code ai.philterd:phileas-pheye-onnx} module; without it there is no way to run a model
 * locally, and falling back to the remote service would silently ignore the policy, so this is a
 * configuration error rather than a recoverable condition.
 *
 * <p>It is raised while the policy's filters are built (the policy-load step), so it surfaces when
 * the policy is first resolved (for example via {@code PlainTextFilterService.prepare(policy)} at
 * startup, or on the first {@code filter()} call for the policy) rather than part-way through
 * processing text. Callers that load policies up front can catch it specifically to tell the user to
 * add the {@code phileas-pheye-onnx} dependency.
 */
public class MissingPhEyeProviderException extends RuntimeException {

    public MissingPhEyeProviderException(final String message) {
        super(message);
    }

}
