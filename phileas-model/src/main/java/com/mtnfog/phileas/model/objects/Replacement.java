package com.mtnfog.phileas.model.objects;

public class Replacement {

    private String replacement;
    private String salt;

    public Replacement(String replacement) {
        this.replacement = replacement;
    }

    public Replacement(String replacement, String salt) {
        this.replacement = replacement;
        this.salt = salt;
    }

    public String getReplacement() {
        return replacement;
    }

    public String getSalt() {
        return salt;
    }

}
