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

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractSpanDisambiguationService {

    private static final Logger LOGGER = LogManager.getLogger(AbstractSpanDisambiguationService.class);

    // Can this vector size be increased over time as the number of documents processed grows?
    // No, because it factors into the hash function.
    // Changing the size would require starting all over because the values in it would
    // no longer be valid because the hash function would have changed.

    private final PhileasConfiguration phileasConfiguration;

    protected final int vectorSize;
    protected final boolean ignoreStopWords;
    protected Set<String> stopwords;
    protected final VectorService vectorService;

    public AbstractSpanDisambiguationService(final PhileasConfiguration phileasConfiguration, final VectorService vectorService) {

        this.phileasConfiguration = phileasConfiguration;
        this.vectorSize = phileasConfiguration.spanDisambiguationVectorSize();
        this.ignoreStopWords = phileasConfiguration.spanDisambiguationIgnoreStopWords();
        this.stopwords = parseStopWords(phileasConfiguration.spanDisambiguationStopWords());
        this.vectorService = vectorService;

    }

    /**
     * Parses the comma-separated stop word list into a set of individual, lower-cased words.
     * Tokens are compared against this set after being lower-cased, so the entries are stored
     * lower-cased here too.
     */
    private static Set<String> parseStopWords(final String stopWords) {

        final Set<String> words = new HashSet<>();

        if (stopWords == null || stopWords.isBlank()) {
            return words;
        }

        for (final String word : stopWords.split(",")) {
            final String trimmed = word.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                words.add(trimmed);
            }
        }

        return words;

    }

    public int hashToken(String token) {

        if(phileasConfiguration.spanDisambiguationHashAlgorithm().equalsIgnoreCase("murmur3")) {
            // Hash the UTF-8 bytes explicitly rather than the platform default charset, so a token
            // hashes to the same index on every platform. This matters because vectors now persist
            // across runs (and potentially across machines); a charset-dependent index would make a
            // persisted store wrong when loaded under a different default charset.
            return Math.abs(MurmurHash3.hash32x86(token.getBytes(StandardCharsets.UTF_8)) % vectorSize);
        } else {
            return Math.abs(token.hashCode() % vectorSize);
        }

    }

}
