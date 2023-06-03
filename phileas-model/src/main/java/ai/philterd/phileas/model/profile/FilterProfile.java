package ai.philterd.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.List;

public class FilterProfile {

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("config")
    @Expose
    private Config config = new Config();

    @SerializedName("crypto")
    @Expose
    private Crypto crypto;

    @SerializedName("fpe")
    @Expose
    private FPE fpe;

    @SerializedName("domain")
    @Expose
    private String domain;

    @SerializedName("identifiers")
    @Expose
    private Identifiers identifiers = new Identifiers();

    @SerializedName("ignored")
    @Expose
    private List<Ignored> ignored = Collections.EMPTY_LIST;

    @SerializedName("ignoredPatterns")
    @Expose
    private List<IgnoredPattern> ignoredPatterns = Collections.EMPTY_LIST;

    @SerializedName("graphical")
    @Expose
    private Graphical graphical = new Graphical();

    @SerializedName("structured")
    @Expose
    private Structured structured;

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).
                append(name).
                append(domain).
                append(crypto).
                append(identifiers).
                append(ignored).
                append(ignoredPatterns).
                append(structured).
                append(config).
                toHashCode();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Structured getStructured() {
        return structured;
    }

    public void setStructured(Structured structured) {
        this.structured = structured;
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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
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