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
package ai.philterd.phileas.utils;

/**
 * Thrown when a value cannot be format-preserving encrypted — for example when its
 * format-preservable content is outside the length range supported by FF3. Callers are expected to
 * fall back to another redaction method for the affected token rather than failing the whole
 * document.
 */
public class FormatPreservingEncryptionException extends RuntimeException {

    public FormatPreservingEncryptionException(final String message) {
        super(message);
    }

    public FormatPreservingEncryptionException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
