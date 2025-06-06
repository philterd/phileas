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

import java.io.IOException;
import java.util.List;

/**
 * Extracts texts from documents.
 */
public interface TextExtractor {

    /**
     * Extracts lines of text from a document.
     * @param document A byte array document.
     * @return A list of lines of text from the document.
     * @throws IOException Thrown if the lines cannot be extracted.
     */
    List<String> getLines(byte[] document) throws IOException;

}
