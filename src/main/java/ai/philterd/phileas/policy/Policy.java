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
package ai.philterd.phileas.policy;

import ai.philterd.phisql.CompileResult;
import ai.philterd.phisql.Compiler;
import ai.philterd.phisql.PhiSQL;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class Policy {

    /**
     * Creates a {@link Policy} from a PhiSQL document. The PhiSQL is compiled to a Phileas JSON
     * policy by the {@code phisql} compiler and then deserialized into a {@link Policy}; the runtime
     * engine is unchanged and still executes JSON. Existing JSON policies continue to load via the
     * usual JSON deserialization — PhiSQL is purely an additional authoring format.
     * @param phisql The PhiSQL document source.
     * @return The compiled {@link Policy}.
     * @throws PolicyCompilationException if the PhiSQL cannot be parsed or compiled.
     */
    public static Policy fromPhiSQL(final String phisql) {

        final CompileResult result;
        try {
            result = new Compiler().compile(phisql);
        } catch (final PhiSQL.ParseException | Compiler.CompileException ex) {
            // Both are unchecked diagnostics from the compiler: ParseException for syntax errors,
            // CompileException for semantic ones (unknown entity type, strategy, and so on). Wrap them
            // in a Phileas type so callers get one exception to catch and the original message is kept.
            throw new PolicyCompilationException("The PhiSQL document could not be compiled into a policy: "
                    + ex.getMessage(), ex);
        }

        return new Gson().fromJson(result.toJsonString(), Policy.class);

    }

    @SerializedName("config")
    @Expose
    private Config config = new Config();

    @SerializedName("crypto")
    @Expose
    private Crypto crypto;

    @SerializedName("fpe")
    @Expose
    private FPE fpe;

    @SerializedName("identifiers")
    @Expose
    private Identifiers identifiers = new Identifiers();

    @SerializedName("ignored")
    @Expose
    private List<Ignored> ignored = Collections.emptyList();

    @SerializedName("ignoredPatterns")
    @Expose
    private List<IgnoredPattern> ignoredPatterns = Collections.emptyList();

    @SerializedName("graphical")
    @Expose
    private Graphical graphical = new Graphical();

    // Memoized cache key. transient/static so it is excluded from JSON serialization and from
    // reflectionEquals.
    private static final Gson CACHE_KEY_GSON = new Gson();
    private transient volatile String cacheKey;

    /**
     * Returns a stable cache key for this policy: an FNV-1a 64-bit hash of its JSON form, computed
     * once and memoized. Reusing one policy across many filter() calls (for example a per-row Spark or
     * Kafka UDF) then avoids re-serializing and re-hashing it on every call.
     *
     * <p>A policy is treated as immutable once it has been used for filtering: the key is computed on
     * first use and is not recomputed for that instance, so mutating the policy afterwards (at any
     * level, top-level or nested) does not change its key and is not supported. To change a policy,
     * build a new one.
     * @return The memoized FNV-1a 64-bit hex key for this policy.
     */
    public String getCacheKey() {
        String key = cacheKey;
        if (key == null) {
            key = fnv1a64(CACHE_KEY_GSON.toJson(this));
            cacheKey = key;
        }
        return key;
    }

    private static String fnv1a64(final String input) {
        final long fnvOffsetBasis = 0xcbf29ce484222325L;
        final long fnvPrime = 0x100000001b3L;
        long hash = fnvOffsetBasis;
        for (final byte b : input.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xff);
            hash *= fnvPrime;
        }
        return Long.toHexString(hash);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).
                append(crypto).
                append(identifiers).
                append(ignored).
                append(ignoredPatterns).
                append(config).
                toHashCode();
    }

    public Identifiers getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Identifiers identifiers) {
        this.identifiers = identifiers;
    }

    public List<Ignored> getIgnored() {
        return ignored;
    }

    public void setIgnored(List<Ignored> ignored) {
        this.ignored = ignored;
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    public List<IgnoredPattern> getIgnoredPatterns() {
        return ignoredPatterns;
    }

    public void setIgnoredPatterns(List<IgnoredPattern> ignoredPatterns) {
        this.ignoredPatterns = ignoredPatterns;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public Graphical getGraphical() {
        return graphical;
    }

    public void setGraphical(Graphical graphical) {
        this.graphical = graphical;
    }

    public FPE getFpe() {
        return fpe;
    }

    public void setFpe(FPE fpe) {
        this.fpe = fpe;
    }

}