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
package ai.philterd.phileas.policy.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Options shared by filters whose values only count after a cue word, such as a member ID after
 * "Member ID".
 */
public abstract class AbstractCueWordFilter extends AbstractFilter {

    /**
     * Regular expressions for the value. When set, they replace the filter's default pattern.
     */
    @SerializedName("patterns")
    @Expose
    private List<String> patterns;

    /**
     * Cue words, added to the filter's default cue words unless {@code defaultCueWords} is false.
     */
    @SerializedName("cueWords")
    @Expose
    private List<String> cueWords;

    /**
     * Whether the filter's default cue words are used along with {@code cueWords}.
     */
    @SerializedName("defaultCueWords")
    @Expose
    private boolean defaultCueWords = true;

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public List<String> getCueWords() {
        return cueWords;
    }

    public void setCueWords(List<String> cueWords) {
        this.cueWords = cueWords;
    }

    public boolean isDefaultCueWords() {
        return defaultCueWords;
    }

    public void setDefaultCueWords(boolean defaultCueWords) {
        this.defaultCueWords = defaultCueWords;
    }

}
