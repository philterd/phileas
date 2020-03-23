package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Crypto {

    @SerializedName("key")
    @Expose
    private String key;

    @SerializedName("iv")
    @Expose
    private String iv;

    /**
     * Empty constructor needed for serialization.
     */
    public Crypto() {

    }

    public Crypto(String key, String iv) {
        this.key = key;
        this.iv = iv;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

}
