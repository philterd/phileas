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
package ai.philterd.phileas.services.filters;

import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.objects.ApplyResponse;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.model.objects.BinaryDocumentFilterResponse;
import ai.philterd.phileas.model.objects.FilterResponse;

import java.util.List;

/**
 * Interface for implementing filter services.
 */
public interface FilterService {

    /**
     * Filter text from a string.
     * @param policy The {@link Policy} to apply.
     * @param input The input text.
     * @return A {@link FilterResponse}.
     * @throws Exception Thrown if the text cannot be filtered.
     */
    FilterResponse filter(final Policy policy, final String context, final String input) throws Exception;

    /**
     * Filter text from a binary document.
     * @param policy The {@link Policy} to apply.s
     * @param input The input document as a byte array.
     * @param mimeType The input {@link MimeType}.
     * @param outputMimeType The output {@link MimeType}.
     * @return A {@link BinaryDocumentFilterResponse}.
     * @throws Exception Thrown if the text cannot be filtered.
     */
    BinaryDocumentFilterResponse filter(final Policy policy, final String context, byte[] input, MimeType mimeType, MimeType outputMimeType) throws Exception;

    /**
     * Applies spans to the input text.
     * @param spans A list of {@link Span spans}.
     * @param input The input text.
     * @param mimeType The input {@link MimeType}.
     * @return A {@link ApplyResponse}.
     */
    ApplyResponse apply(final List<Span> spans, final String input, final MimeType mimeType);

}
