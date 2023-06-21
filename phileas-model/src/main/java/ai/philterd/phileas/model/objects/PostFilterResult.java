/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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

public class PostFilterResult {

    private boolean isPostFiltered;
    private Span span;

    public PostFilterResult(Span span, boolean isPostFiltered) {
        this.span = span;
        this.isPostFiltered = isPostFiltered;
    }

    public boolean isPostFiltered() {
        return isPostFiltered;
    }

    public Span getSpan() {
        return span;
    }

}
