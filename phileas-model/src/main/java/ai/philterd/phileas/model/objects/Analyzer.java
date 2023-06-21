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
package ai.philterd.phileas.model.objects;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Analyzer {

    private Set<String> contextualTerms;
    private List<FilterPattern> filterPatterns;

    public Analyzer(FilterPattern ... p) {

        this.filterPatterns = new LinkedList<>();

        for(final FilterPattern filterPattern : p) {
            filterPatterns.add(filterPattern);
        }

    }

    public Analyzer(Set<String> contextualTerms, FilterPattern ... p) {

        this.contextualTerms = contextualTerms;
        this.filterPatterns = new LinkedList<>();

        for(final FilterPattern filterPattern : p) {
            filterPatterns.add(filterPattern);
        }

    }

    public Analyzer(List<FilterPattern> filterPatterns) {

        this.filterPatterns = filterPatterns;

    }

    public Analyzer(Set<String> contextualTerms, List<FilterPattern> filterPatterns) {

        this.contextualTerms = contextualTerms;
        this.filterPatterns = filterPatterns;

    }

    public List<FilterPattern> getFilterPatterns() {
        return filterPatterns;
    }

    public Set<String> getContextualTerms() {
        return contextualTerms;
    }

}
