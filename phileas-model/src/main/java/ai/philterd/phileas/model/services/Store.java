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

import ai.philterd.phileas.model.objects.Span;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * A database store of {@link Span spans}.
 */
public interface Store extends Serializable {

    /**
     * Inserts a {@link Span span} in the database
     * @param span The {@link Span span} to store.
     */
    void insert(Span span) throws IOException;

    /**
     * Inserts many {@link Span spans} in the database
     * @param spans The {@link Span spans} to store.
     */
    void insert(List<Span> spans) throws IOException;

    /**
     * Gets a list of spans for a document ID.
     * @param documentId The document ID.
     * @return A list of matching {@link Span spans}.
     */
    List<Span> getByDocumentId(String documentId) throws IOException;

}
