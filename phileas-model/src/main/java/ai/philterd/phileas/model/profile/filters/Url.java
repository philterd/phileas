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
package ai.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.profile.filters.strategies.rules.UrlFilterStrategy;

import java.util.List;

public class Url extends AbstractFilter {

    @SerializedName("urlFilterStrategies")
    @Expose
    private List<UrlFilterStrategy> urlFilterStrategies;

    @SerializedName("requireHttpWwwPrefix")
    @Expose
    private boolean requireHttpWwwPrefix = true;

    public List<UrlFilterStrategy> getUrlFilterStrategies() {
        return urlFilterStrategies;
    }

    public void setUrlFilterStrategies(List<UrlFilterStrategy> urlFilterStrategies) {
        this.urlFilterStrategies = urlFilterStrategies;
    }

    public boolean isRequireHttpWwwPrefix() {
        return requireHttpWwwPrefix;
    }

    public void setRequireHttpWwwPrefix(boolean requireHttpWwwPrefix) {
        this.requireHttpWwwPrefix = requireHttpWwwPrefix;
    }

}