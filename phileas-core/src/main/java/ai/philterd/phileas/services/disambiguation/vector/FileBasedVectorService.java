/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.disambiguation.vector;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.SpanVector;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link VectorService} that persists the accumulated disambiguation vectors to a file so the
 * learning survives process restarts. This is what makes the "improves over time" behavior durable;
 * the in-memory store alone resets every time the process stops.
 *
 * <p>The accumulated vectors are held in memory (inheriting the thread-safe accumulation of
 * {@link InMemoryVectorService}) and are loaded from the file on construction. Because writing the
 * whole store on every token insert would dominate filtering throughput, persistence is explicit:
 * call {@link #save()} on whatever cadence fits (for example periodically and/or at shutdown), or
 * use try-with-resources / {@link #close()} to save once at the end.
 *
 * <p>Stored vectors are only meaningful for the exact vector size and hash algorithm they were built
 * with: the vector size bounds every index and the hash algorithm decides which index a token maps
 * to, so changing either makes previously-stored counts garbage. The file therefore records both,
 * and a load whose parameters do not match the configured ones (or whose format version is unknown)
 * is discarded with a warning and treated as a cold start, rather than silently producing wrong
 * similarities. The matching values are written back on the next {@link #save()}.
 */
public class FileBasedVectorService extends InMemoryVectorService implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger(FileBasedVectorService.class);

    /** Bumped if the on-disk layout changes incompatibly. */
    private static final int FORMAT_VERSION = 1;

    private final Path path;
    private final Gson gson;
    private final int vectorSize;
    private final String hashAlgorithm;

    /**
     * Creates the service backed by the given file, loading any previously-saved vectors that were
     * built with the same vector size and hash algorithm.
     * @param path The file used to persist and restore the vectors.
     * @param vectorSize The configured disambiguation vector size (e.g. {@code span.disambiguation.vector.size}).
     * @param hashAlgorithm The configured hash algorithm (e.g. {@code span.disambiguation.hash.algorithm}).
     * @throws IOException If an existing file cannot be read or parsed.
     */
    public FileBasedVectorService(final Path path, final int vectorSize, final String hashAlgorithm) throws IOException {
        super();
        this.path = path;
        this.gson = new Gson();
        this.vectorSize = vectorSize;
        this.hashAlgorithm = normalizeAlgorithm(hashAlgorithm);
        load();
    }

    /**
     * Normalizes the configured algorithm name to its effective behavior. Anything that is not
     * (case-insensitively) {@code murmur3} falls back to {@code String.hashCode()}, mirroring
     * {@code AbstractSpanDisambiguationService}, so two names that mean the same thing are not
     * treated as a mismatch.
     */
    private static String normalizeAlgorithm(final String hashAlgorithm) {
        return hashAlgorithm != null && hashAlgorithm.equalsIgnoreCase("murmur3") ? "murmur3" : "hashCode";
    }

    /**
     * Loads the persisted vectors into the in-memory cache. A missing or empty file is a cold start,
     * and a file whose parameters do not match the configured ones is discarded (also a cold start).
     */
    private void load() throws IOException {

        if(path == null || !Files.exists(path) || Files.size(path) == 0) {
            LOGGER.debug("No persisted disambiguation vectors at {}; starting cold.", path);
            return;
        }

        final String json = Files.readString(path, StandardCharsets.UTF_8);
        final PersistedVectors persisted = gson.fromJson(json, PersistedVectors.class);

        if(persisted == null || persisted.vectors == null) {
            return;
        }

        // Refuse to load vectors that were built with a different size/algorithm/format: their index
        // values would be meaningless under the current configuration.
        final String persistedAlgorithm = normalizeAlgorithm(persisted.hashAlgorithm);
        if(persisted.version != FORMAT_VERSION
                || persisted.vectorSize != vectorSize
                || !persistedAlgorithm.equals(hashAlgorithm)) {

            LOGGER.warn("Discarding persisted disambiguation vectors at {}: incompatible parameters "
                            + "(file version={}, size={}, algorithm={}; expected version={}, size={}, algorithm={}). "
                            + "Starting cold; a compatible store will be written on the next save.",
                    path, persisted.version, persisted.vectorSize, persistedAlgorithm,
                    FORMAT_VERSION, vectorSize, hashAlgorithm);
            return;
        }

        for(final Map.Entry<String, Map<FilterType, Map<Double, Double>>> contextEntry : persisted.vectors.entrySet()) {

            // Start from a fully-populated context (all filter types present) so reads are never null.
            final Map<FilterType, SpanVector> contextVectors = new HashMap<>();
            for(final FilterType filterType : FilterType.values()) {
                contextVectors.put(filterType, new SpanVector());
            }

            for(final Map.Entry<FilterType, Map<Double, Double>> filterTypeEntry : contextEntry.getValue().entrySet()) {

                // Rebuild the index map as a ConcurrentHashMap; Gson would otherwise hand back a
                // non-thread-safe map that later accumulation would mutate unsafely.
                final SpanVector spanVector = contextVectors.get(filterTypeEntry.getKey());
                if(spanVector != null && filterTypeEntry.getValue() != null) {
                    spanVector.setVectorIndexes(new ConcurrentHashMap<>(filterTypeEntry.getValue()));
                }

            }

            vectorCache.put(contextEntry.getKey(), contextVectors);

        }

        LOGGER.debug("Loaded persisted disambiguation vectors for {} context(s) from {}.", vectorCache.size(), path);

    }

    /**
     * Persists the current vectors to the file, recording the vector size and hash algorithm they
     * were built with. The write goes to a temporary file that is then atomically moved into place,
     * so a crash mid-write cannot corrupt an existing store.
     * @throws IOException If the file cannot be written.
     */
    public synchronized void save() throws IOException {

        // Build a plain DTO (index map values) from the live cache. ConcurrentHashMap iteration is
        // weakly consistent, which is acceptable for this approximate accumulator.
        final Map<String, Map<FilterType, Map<Double, Double>>> vectors = new HashMap<>();

        for(final Map.Entry<String, Map<FilterType, SpanVector>> contextEntry : vectorCache.entrySet()) {

            final Map<FilterType, Map<Double, Double>> contextSnapshot = new HashMap<>();

            for(final Map.Entry<FilterType, SpanVector> filterTypeEntry : contextEntry.getValue().entrySet()) {

                final Map<Double, Double> indexes = filterTypeEntry.getValue().getVectorIndexes();

                // Only persist filter types that actually accumulated something, to keep the file small.
                if(!indexes.isEmpty()) {
                    contextSnapshot.put(filterTypeEntry.getKey(), new HashMap<>(indexes));
                }

            }

            if(!contextSnapshot.isEmpty()) {
                vectors.put(contextEntry.getKey(), contextSnapshot);
            }

        }

        final PersistedVectors persisted = new PersistedVectors(FORMAT_VERSION, vectorSize, hashAlgorithm, vectors);

        if(path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        final Path temp = Files.createTempFile(
                path.getParent() == null ? path.toAbsolutePath().getParent() : path.getParent(),
                "vectors", ".json.tmp");

        try {
            Files.writeString(temp, gson.toJson(persisted), StandardCharsets.UTF_8);
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (final IOException ex) {
            Files.deleteIfExists(temp);
            throw ex;
        }

    }

    /**
     * Saves the vectors. Allows the service to be used in a try-with-resources block.
     */
    @Override
    public void close() throws IOException {
        save();
    }

    /**
     * The on-disk representation: the vectors plus the parameters they were built with. Fields are
     * read reflectively by Gson.
     */
    private static final class PersistedVectors {

        private final int version;
        private final int vectorSize;
        private final String hashAlgorithm;
        private final Map<String, Map<FilterType, Map<Double, Double>>> vectors;

        private PersistedVectors(final int version, final int vectorSize, final String hashAlgorithm,
                                 final Map<String, Map<FilterType, Map<Double, Double>>> vectors) {
            this.version = version;
            this.vectorSize = vectorSize;
            this.hashAlgorithm = hashAlgorithm;
            this.vectors = vectors;
        }

    }

}
