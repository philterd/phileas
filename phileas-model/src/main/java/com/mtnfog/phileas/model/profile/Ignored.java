package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class Ignored {

    private static final Logger LOGGER = LogManager.getLogger(Ignored.class);

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("terms")
    @Expose
    private List<String> terms;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

}
