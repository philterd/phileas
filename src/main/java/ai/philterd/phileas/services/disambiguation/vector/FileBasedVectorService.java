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
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
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
 * <p>The on-disk format is JSON: {@code context -> filterType -> (vector index -> count)}. Loading
 * tolerates a missing or empty file (cold start) and rebuilds every vector's index map as a
 * {@link ConcurrentHashMap} so subsequent accumulation remains thread-safe.
 */
public class FileBasedVectorService extends InMemoryVectorService implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger(FileBasedVectorService.class);

    /** context -> filterType -> (vector index -> count). */
    private static final Type PERSISTENCE_TYPE =
            new TypeToken<Map<String, Map<FilterType, Map<Double, Double>>>>(){}.getType();

    private final Path path;
    private final Gson gson;

    /**
     * Creates the service backed by the given file, loading any previously-saved vectors.
     * @param path The file used to persist and restore the vectors.
     * @throws IOException If an existing file cannot be read or parsed.
     */
    public FileBasedVectorService(final Path path) throws IOException {
        super();
        this.path = path;
        this.gson = new Gson();
        load();
    }

    /**
     * Loads the persisted vectors into the in-memory cache. A missing or empty file is treated as a
     * cold start (nothing to load).
     */
    private void load() throws IOException {

        if(path == null || !Files.exists(path) || Files.size(path) == 0) {
            LOGGER.debug("No persisted disambiguation vectors at {}; starting cold.", path);
            return;
        }

        final String json = Files.readString(path, StandardCharsets.UTF_8);
        final Map<String, Map<FilterType, Map<Double, Double>>> persisted = gson.fromJson(json, PERSISTENCE_TYPE);

        if(persisted == null) {
            return;
        }

        for(final Map.Entry<String, Map<FilterType, Map<Double, Double>>> contextEntry : persisted.entrySet()) {

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
     * Persists the current vectors to the file. The write goes to a temporary file that is then
     * atomically moved into place, so a crash mid-write cannot corrupt an existing store.
     * @throws IOException If the file cannot be written.
     */
    public synchronized void save() throws IOException {

        // Build a plain DTO (index map values) from the live cache. ConcurrentHashMap iteration is
        // weakly consistent, which is acceptable for this approximate accumulator.
        final Map<String, Map<FilterType, Map<Double, Double>>> snapshot = new HashMap<>();

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
                snapshot.put(contextEntry.getKey(), contextSnapshot);
            }

        }

        if(path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        final Path temp = Files.createTempFile(
                path.getParent() == null ? path.toAbsolutePath().getParent() : path.getParent(),
                "vectors", ".json.tmp");

        try {
            Files.writeString(temp, gson.toJson(snapshot, PERSISTENCE_TYPE), StandardCharsets.UTF_8);
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

}
