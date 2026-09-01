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
package ai.philterd.phileas.policy;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

public class IgnoredPattern {

    private static final Logger LOGGER = LogManager.getLogger(IgnoredPattern.class);

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("pattern")
    @Expose
    private String pattern;

    // The compiled form of the pattern, cached so a Pattern is not compiled on every match. It is
    // built lazily (Gson sets the pattern field directly, bypassing the setter) and invalidated
    // whenever the pattern changes.
    private transient Pattern compiledPattern;

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
        this.compiledPattern = null;
    }

    /**
     * Determines whether the input matches this ignored pattern, using a cached compiled
     * {@link Pattern} so the regex is compiled at most once per pattern value.
     * @param input The text to test.
     * @return <code>true</code> if the input fully matches the pattern.
     */
    public boolean matches(final String input) {
        Pattern p = compiledPattern;
        if (p == null) {
            p = Pattern.compile(pattern);
            compiledPattern = p;
        }
        try {
            return p.matcher(input).matches();
        } catch (final StackOverflowError e) {
            // The pattern comes from the policy, and java.util.regex compiles a non-atomic repeated
            // group into a recursive call per character, so matching a long token overflows the
            // stack. Without this the Error ends filtering for the whole document. Treating the
            // token as not ignored is the safe answer: it stays a span and is redacted.
            LOGGER.warn("Ignored pattern {} overflowed the stack on a {} character token; "
                    + "treating the token as not ignored.", pattern, input.length());
            return false;
        }
    }

}
