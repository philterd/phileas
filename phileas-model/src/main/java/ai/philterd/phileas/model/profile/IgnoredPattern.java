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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class IgnoredPattern {

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("pattern")
    @Expose
    private String pattern;

    public IgnoredPattern() {

    }

    public IgnoredPattern(String name, String pattern) {
        this.name = name;
        this.pattern = pattern;
    }

    public IgnoredPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return pattern;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.name)
                .append(this.pattern)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof IgnoredPattern) {

            final IgnoredPattern ip = (IgnoredPattern) obj;

            return (StringUtils.equals(ip.name, this.name) && StringUtils.equals(ip.pattern, this.pattern));

        }

        return false;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

}
