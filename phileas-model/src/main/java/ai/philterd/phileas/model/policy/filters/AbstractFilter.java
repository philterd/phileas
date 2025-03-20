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
package ai.philterd.phileas.model.policy.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.policy.IgnoredPattern;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class AbstractFilter {

    @SerializedName("enabled")
    @Expose
    protected boolean enabled = true;

    @SerializedName("ignored")
    @Expose
    protected Set<String> ignored = Collections.emptySet();

    @SerializedName("ignoredFiles")
    @Expose
    protected Set<String> ignoredFiles = Collections.emptySet();

    @SerializedName("ignoredPatterns")
    @Expose
    protected List<IgnoredPattern> ignoredPatterns = Collections.emptyList();

<<<<<<< HEAD
    @SerializedName("windowSize")
    @Expose
    protected int windowSize;
=======
    @SerializedName("priority")
    @Expose
    protected int priority;
>>>>>>> main

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setIgnored(Set<String> ignored) {
        this.ignored = ignored;
    }

    public Set<String> getIgnored() {
        return ignored;
    }

    public List<IgnoredPattern> getIgnoredPatterns() {
        return ignoredPatterns;
    }

    public void setIgnoredPatterns(List<IgnoredPattern> ignoredPatterns) {
        this.ignoredPatterns = ignoredPatterns;
    }

    public void setIgnoredFiles(Set<String> ignoredFiles) {
        this.ignoredFiles = ignoredFiles;
    }

    public Set<String> getIgnoredFiles() {
        return ignoredFiles;
    }

<<<<<<< HEAD
    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public int getWindowSizeOrDefault(final int defaultWindowSize) {
        if (windowSize == 0) {
            return defaultWindowSize;
        }
        return windowSize;
=======
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
>>>>>>> main
    }

}
