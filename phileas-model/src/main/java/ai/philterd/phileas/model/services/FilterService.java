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

import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.responses.BinaryDocumentFilterResponse;
import ai.philterd.phileas.model.responses.FilterResponse;

import java.util.List;

/**
 * Interface for implementing filter services.
 */
public interface FilterService {

    /**
     * Filter text from a string.
     * @param policyNames The list of the policies to use.
     * @param context The context.
     * @param documentId A document ID. Provide <code>null</code> for a document ID to be generated.
     * @param input The input text.
     * @param mimeType The {@link MimeType}.
     * @return A {@link FilterResponse}.
     * @throws Exception Thrown if the text cannot be filtered.
     */
    FilterResponse filter(List<String> policyNames, String context, String documentId, String input, MimeType mimeType) throws Exception;

    /**
     * Filter text from a binary document.
     * @param policyNames The list of the policies to use.
     * @param context The context.
     * @param documentId A document ID. Provide <code>null</code> for a document ID to be generated.
     * @param input The input document as a byte array.
     * @param mimeType The input {@link MimeType}.
     * @param outputMimeType The output {@link MimeType}.
     * @return A {@link BinaryDocumentFilterResponse}.
     * @throws Exception Thrown if the text cannot be filtered.
     */
    BinaryDocumentFilterResponse filter(List<String> policyNames, String context, String documentId, byte[] input, MimeType mimeType, MimeType outputMimeType) throws Exception;

    /**
     * Gets the policy service being used.
     * @return A {@link PolicyService}.
     */
    PolicyService getPolicyService();

    /**
     * Returns the alert service being used.
     * @return An {@link AlertService}.
     */
    AlertService getAlertService();

}
