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
package ai.philterd.phileas.model.objects;

import java.util.List;

public class FilterResult {

    private final String documentId;
    private final String context;
    private int piece;
    private final List<Span> spans;

    public FilterResult(final String context, final String documentId, final List<Span> spans) {

        this.documentId = documentId;
        this.context = context;
        this.piece = 0;
        this.spans = spans;

    }

    public FilterResult(String context, String documentId, int piece, List<Span> spans) {

        this.documentId = documentId;
        this.context = context;
        this.spans = spans;

    }

    public String getDocumentId() {
        return documentId;
    }

    public String getContext() {
        return context;
    }

    public int getPiece() {
        return piece;
    }

    public List<Span> getSpans() {
        return spans;
    }

}
