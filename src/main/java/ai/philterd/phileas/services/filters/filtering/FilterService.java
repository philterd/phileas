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
package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.model.filtering.AbstractFilterResult;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.policy.Ignored;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.FilterPolicyLoader;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.filters.postfilters.IgnoredPatternsFilter;
import ai.philterd.phileas.services.filters.postfilters.IgnoredTermsFilter;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.filters.postfilters.TrailingNewLinePostFilter;
import ai.philterd.phileas.services.filters.postfilters.TrailingPeriodPostFilter;
import ai.philterd.phileas.services.filters.postfilters.TrailingSpacePostFilter;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class FilterService<T extends AbstractFilterResult> {

    protected final FilterPolicyLoader filterPolicyLoader;

    // A map that gives each filter profile its own cache of filters.
    protected final Map<String, Map<FilterType, Filter>> filterCache;

    public FilterService(final PhileasConfiguration phileasConfiguration,
                         final ContextService contextService) {

        this.filterCache = new ConcurrentHashMap<>();
        this.filterPolicyLoader = new FilterPolicyLoader(contextService, phileasConfiguration);

    }

    protected List<PostFilter> getPostFiltersForPolicy(final Policy policy) throws IOException {

        final List<PostFilter> postFilters = new LinkedList<>();

        // Ignored terms filter. Looks for ignored terms in the scope of the whole document (and not just a particular filter).
        // No matter what filter found the span, it is subject to this ignore list.
        if(CollectionUtils.isNotEmpty(policy.getIgnored())) {

            // Make a post-filter for each Ignored item in the list.
            for(final Ignored ignored : policy.getIgnored()) {
                postFilters.add(new IgnoredTermsFilter(ignored));
            }

        }

        // Ignored patterns filter. Looks for terms matching a pattern in the scope of the whole document (and not just a particular filter).
        // No matter what filter found the span, it is subject to this ignore list.
        if(CollectionUtils.isNotEmpty(policy.getIgnoredPatterns())) {
            postFilters.add(new IgnoredPatternsFilter(policy.getIgnoredPatterns()));
        }

        // Add the post-filters if they are enabled in the policy.

        if(policy.getConfig().getPostFilters().isRemoveTrailingPeriods()) {
            postFilters.add(TrailingPeriodPostFilter.getInstance());
        }

        if(policy.getConfig().getPostFilters().isRemoveTrailingSpaces()) {
            postFilters.add(TrailingSpacePostFilter.getInstance());
        }

        if(policy.getConfig().getPostFilters().isRemoveTrailingNewLines()) {
            postFilters.add(TrailingNewLinePostFilter.getInstance());
        }

        return postFilters;

    }

}
