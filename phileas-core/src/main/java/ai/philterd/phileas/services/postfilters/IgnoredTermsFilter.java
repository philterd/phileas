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
package ai.philterd.phileas.services.postfilters;

import ai.philterd.phileas.model.objects.PostFilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Ignored;
import ai.philterd.phileas.model.services.PostFilter;
import ai.philterd.phileas.model.utils.BloomFilter;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link PostFilter} that removes identified
 * tokens found in an ignore list.
 */
public class IgnoredTermsFilter extends PostFilter {

    private static final Logger LOGGER = LogManager.getLogger(IgnoredTermsFilter.class);

    private final BloomFilter<String> bloomFilter;
    private final Set<String> ignoredTerms = new HashSet<>();
    private final Ignored ignored;

    public IgnoredTermsFilter(final Ignored ignored) throws IOException {

        this.ignored = ignored;

        // Read the ignored terms from the files.
        final Set<String> ignoredTermsFromFiles = new HashSet<>();

        for(final String file : ignored.getFiles()) {
            ignoredTermsFromFiles.addAll(FileUtils.readLines(new File(file), Charset.defaultCharset()));
        }

        if(ignored.isCaseSensitive()) {

            ignoredTerms.addAll(ignored.getTerms());
            ignoredTerms.addAll(ignoredTermsFromFiles);

        } else {

            LOGGER.debug("Ignore terms is not case sensitive.");

            // Not case-sensitive. Lowercase everything before adding.
            ignoredTerms.addAll(ignored.getTerms().stream()
                    .map(String::toLowerCase)
                    .toList());

            ignoredTerms.addAll(ignoredTermsFromFiles.stream()
                    .map(String::toLowerCase)
                    .toList());

        }

        this.bloomFilter = new BloomFilter<>(ignoredTerms);

        LOGGER.info("Added {} terms to ignore.", ignoredTerms.size());

    }

    @Override
    protected PostFilterResult process(final String text, final Span span) {

        String spanText = span.getText(text);

        if(!ignored.isCaseSensitive()) {
            spanText = spanText.toLowerCase();
        }

        final boolean mightContain = bloomFilter.mightContain(spanText);

        final boolean ignored;

        if(mightContain) {
            ignored = ignoredTerms.contains(spanText);
        } else {
            ignored = false;
        }

        // Return false if allowed; true if ignored.
        return new PostFilterResult(span, ignored);

    }

}
