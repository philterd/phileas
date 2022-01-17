package com.mtnfog.phileas.service.ai;

public class Tokens {

    private String[] tokens;
    private long[] ids;
    private long[] mask;
    private long[] types;

    public Tokens(String[] tokens, long[] ids, long[] mask, long[] types) {

        this.tokens = tokens;
        this.ids = ids;
        this.mask = mask;
        this.types = types;

    }

    public String[] getTokens() {
        return tokens;
    }

    public long[] getIds() {
        return ids;
    }

    public long[] getMask() {
        return mask;
    }


    public long[] getTypes() {
        return types;
    }

}