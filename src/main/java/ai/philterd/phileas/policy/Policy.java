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