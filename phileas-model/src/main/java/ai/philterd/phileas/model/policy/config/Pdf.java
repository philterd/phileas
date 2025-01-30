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
package ai.philterd.phileas.model.policy.config;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Pdf {

    @SerializedName("redactionColor")
    @Expose
    private String redactionColor = "black";

    @SerializedName("showReplacement")
    @Expose
    private boolean showReplacement = false;

    @SerializedName("replacementFont")
    @Expose
    private String replacementFont = "helvetica";

    @SerializedName("replacementMaxFontSize")
    @Expose
    private float replacementMaxFontSize = 12;

    @SerializedName("replacementFontColor")
    @Expose
    private String replacementFontColor;

    @SerializedName("scale")
    @Expose
    private float scale = 1.0f;

    @SerializedName("dpi")
    @Expose
    private int dpi = 150;

    @SerializedName("compressionQuality")
    @Expose
    private float compressionQuality = 1.0f;

    public String getRedactionColor() {
        return redactionColor;
    }

    public void setRedactionColor(String replacementColor) {
        this.redactionColor = replacementColor;
    }

    public String getReplacementFont() {
        return replacementFont;
    }

    public void setReplacementFont(String replacementFont) {
        this.replacementFont = replacementFont;
    }

    public float getReplacementMaxFontSize() {
        return replacementMaxFontSize;
    }

    public String getReplacementFontColor() {
        return replacementFontColor;
    }

    public boolean getShowReplacement() {
        return showReplacement;
    }

    public void setShowReplacement(boolean showReplacement) {
        this.showReplacement = showReplacement;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scaling) {
        this.scale = scaling;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public float getCompressionQuality() {
        return compressionQuality;
    }

    public void setCompressionQuality(float compressionQuality) {
        this.compressionQuality = compressionQuality;
    }
}
