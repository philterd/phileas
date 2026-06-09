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
package ai.philterd.phileas.policy;

/**
 * Thrown when a PhiSQL document cannot be compiled into a {@link Policy} — either because it is not
 * valid PhiSQL syntax or because it is syntactically valid but semantically invalid (an unknown
 * entity type, strategy, and so on). The message carries the underlying compiler diagnostics so the
 * caller can surface them to whoever authored the PhiSQL.
 */
public class PolicyCompilationException extends RuntimeException {

    public PolicyCompilationException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
