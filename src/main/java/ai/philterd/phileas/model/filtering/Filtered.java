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
package ai.philterd.phileas.model.filtering;

import java.util.List;

public class Filtered {

    private final String context;
    private final int piece;
    private final List<Span> spans;

    public Filtered(final String context, final List<Span> spans) {

        this.context = context;
        this.piece = 0;
        this.spans = spans;

    }

    public Filtered(String context, int piece, List<Span> spans) {

        this.context = context;
        this.piece = piece;
        this.spans = spans;

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
