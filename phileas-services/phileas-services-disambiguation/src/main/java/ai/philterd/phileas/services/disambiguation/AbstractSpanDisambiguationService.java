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
package ai.philterd.phileas.services.disambiguation;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;

import ai.philterd.phileas.model.services.CacheService;
import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractSpanDisambiguationService {

    private static final Logger LOGGER = LogManager.getLogger(AbstractSpanDisambiguationService.class);

    // Can this vector size be increased over time as the number of documents process grows?
    // No, because it factors into the hash function.
    // Changing the size would require starting all over because the values in it would
    // no longer be valid because the hash function would have changed.

    private final PhileasConfiguration phileasConfiguration;

    protected boolean enabled;
    protected final int vectorSize;
    protected final boolean ignoreStopWords;
    protected CacheService cacheService;
    protected Set<String> stopwords;

    public AbstractSpanDisambiguationService(final PhileasConfiguration phileasConfiguration, final CacheService cacheService) throws IOException {

        this.phileasConfiguration = phileasConfiguration;
        this.vectorSize = phileasConfiguration.spanDisambiguationVectorSize();
        this.ignoreStopWords = phileasConfiguration.spanDisambiguationIgnoreStopWords();
        this.stopwords = new HashSet<>(Arrays.asList(phileasConfiguration.spanDisambiguationStopWords().split("")));
        this.cacheService = cacheService;
        this.enabled = phileasConfiguration.spanDisambiguationEnabled();

    }

    public int hashToken(String token) {

        if(StringUtils.equalsIgnoreCase(phileasConfiguration.spanDisambiguationHashAlgorithm(), "murmu3")) {
            return Math.abs(MurmurHash3.hash32x86(token.getBytes()) % vectorSize);
        } else {
            return Math.abs(token.hashCode() % vectorSize);
        }

    }

    // TODO: I don't like this. I did this because the SpanDisambiguationService has to be created
    // before a boolean check to determine if the service is actually enabled. Making an
    // implementation of SpanDisambiguationService that does nothing seemed like a really
    // bad idea so I went this route instead. It needs worked on from service instantiation
    // up to service use.
    public boolean isEnabled() {
        return enabled;
    }

}
