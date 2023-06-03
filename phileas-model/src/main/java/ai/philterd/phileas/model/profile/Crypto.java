package ai.philterd.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;

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

        if(key.startsWith("env:")) {

            final String envVarName = key.substring(4);
            return System.getenv(envVarName);

        }

        return key;

    }

    public String getIv() {

        if(iv.startsWith("env:")) {

            final String envVarName = iv.substring(4);
            return System.getenv(envVarName);

        }

        return iv;

    }

}
