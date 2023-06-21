/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PostFilters {

    @SerializedName("removeTrailingPeriods")
    @Expose
    private boolean removeTrailingPeriods = true;

    @SerializedName("removeTrailingSpaces")
    @Expose
    private boolean removeTrailingSpaces = true;

    @SerializedName("removeTrailingNewLines")
    @Expose
    private boolean removeTrailingNewLines = true;

    public boolean isRemoveTrailingPeriods() {
        return removeTrailingPeriods;
    }

    public void setRemoveTrailingPeriods(boolean removeTrailingPeriods) {
        this.removeTrailingPeriods = removeTrailingPeriods;
    }

    public boolean isRemoveTrailingSpaces() {
        return removeTrailingSpaces;
    }

    public void setRemoveTrailingSpaces(boolean removeTrailingSpaces) {
        this.removeTrailingSpaces = removeTrailingSpaces;
    }

    public boolean isRemoveTrailingNewLines() {
        return removeTrailingNewLines;
    }

    public void setRemoveTrailingNewLines(boolean removeTrailingNewLines) {
        this.removeTrailingNewLines = removeTrailingNewLines;
    }

}
