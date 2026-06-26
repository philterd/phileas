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
import ai.philterd.phileas.policy.Ignored;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.FilterPolicyLoader;
import ai.philterd.phileas.services.filters.postfilters.IgnoredPatternsFilter;
import ai.philterd.phileas.services.filters.postfilters.IgnoredTermsFilter;
import ai.philterd.phileas.services.filters.postfilters.PostFilter;
import ai.philterd.phileas.services.filters.postfilters.TrailingNewLinePostFilter;
import ai.philterd.phileas.services.filters.postfilters.TrailingPeriodPostFilter;
import ai.philterd.phileas.services.filters.postfilters.TrailingSpacePostFilter;
import ai.philterd.phileas.utils.CollectionUtils;
import org.apache.hc.client5.http.classic.HttpClient;

import java.security.SecureRandom;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for filter services.
 *
 * <p>A warm instance is safe to share across threads: the filter and post-filter caches are
 * {@link ConcurrentHashMap}s populated via {@code computeIfAbsent}, and {@code filter()} does not
 * mutate instance state. The per-request context and vector services are supplied per call rather
 * than held on the instance, so concurrent callers do not share that state. The {@link SecureRandom}
 * used for anonymization is supplied at construction and shared across calls, so it must be
 * thread-safe (the default {@link SecureRandom} is). Per-row callers (Spark, Kafka, logging UDFs)
 * should share one instance rather than locking around {@code filter()}.
 */
public abstract class FilterService {

    protected final FilterPolicyLoader filterPolicyLoader;

    // Caches the complete list of filters built for a policy, keyed by a hash of the policy.
    protected final Map<String, List<Filter>> filterCache;

    // Caches the post-filters built for a policy, keyed by the same policy hash as filterCache.
    protected final Map<String, List<PostFilter>> postFilterCache;

    protected FilterService(final PhileasConfiguration phileasConfiguration,
                            final SecureRandom random,
                            final HttpClient httpClient) {

        this.filterCache = new ConcurrentHashMap<>();
        this.postFilterCache = new ConcurrentHashMap<>();
        this.filterPolicyLoader = new FilterPolicyLoader(phileasConfiguration, random, httpClient);

    }

    protected List<PostFilter> getPostFiltersForPolicy(final Policy policy) throws IOException {

        // Build the post-filter list at most once per policy and reuse it, mirroring the filter cache.
        // The build throws a checked IOException, which a mapping function cannot, so it is wrapped in
        // a CompletionException and unwrapped here to preserve this method's throws contract.
        try {
            return postFilterCache.computeIfAbsent(policy.getCacheKey(), key -> {
                try {
                    return buildPostFilters(policy);
                } catch (final IOException e) {
                    throw new CompletionException(e);
                }
            });
        } catch (final CompletionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw e;
        }

    }

    private List<PostFilter> buildPostFilters(final Policy policy) throws IOException {

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
