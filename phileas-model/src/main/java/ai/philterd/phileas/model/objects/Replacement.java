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
package ai.philterd.phileas.model.objects;

public class Replacement {

    private final String replacement;
    private String salt;
    private boolean applied = true;

    public Replacement(final String replacement) {
        this.replacement = replacement;
    }

    public Replacement(final String replacement, final boolean applied) {
        this.replacement = replacement;
        this.applied = applied;
    }

    public Replacement(final String replacement, final String salt) {
        this.replacement = replacement;
        this.salt = salt;
    }

    public String getReplacement() {
        return replacement;
    }

    public String getSalt() {
        return salt;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

}
