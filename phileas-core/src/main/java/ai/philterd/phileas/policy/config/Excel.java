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
package ai.philterd.phileas.policy.config;

import ai.philterd.phileas.policy.config.Word.RedactOrKeep;
import ai.philterd.phileas.policy.config.Word.RemoveOrKeep;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * The policy's {@code config.excel}: how an Excel workbook is redacted beyond its cell text. Cell text and comments
 * are always redacted by the policy's identifiers, and authorship is always cleared.
 */
public class Excel {

    @SerializedName("printHeadersFooters")
    @Expose
    private RedactOrKeep printHeadersFooters = RedactOrKeep.REDACT;

    @SerializedName("charts")
    @Expose
    private RedactOrKeep charts = RedactOrKeep.REDACT;

    @SerializedName("formulaCaches")
    @Expose
    private RedactOrKeep formulaCaches = RedactOrKeep.REDACT;

    @SerializedName("pivotCaches")
    @Expose
    private RedactOrKeep pivotCaches = RedactOrKeep.REDACT;

    @SerializedName("uninspectableObjects")
    @Expose
    private RemoveOrKeep uninspectableObjects = RemoveOrKeep.REMOVE;

    @SerializedName("removeDocumentProperties")
    @Expose
    private boolean removeDocumentProperties = true;

    /** {@code redact} redacts print headers and footers; {@code keep} leaves them unscanned. */
    public RedactOrKeep getPrintHeadersFooters() {
        return printHeadersFooters;
    }

    public Excel setPrintHeadersFooters(final RedactOrKeep printHeadersFooters) {
        this.printHeadersFooters = printHeadersFooters;
        return this;
    }

    /** {@code redact} redacts chart text and cached values; {@code keep} leaves charts as they are. */
    public RedactOrKeep getCharts() {
        return charts;
    }

    public Excel setCharts(final RedactOrKeep charts) {
        this.charts = charts;
        return this;
    }

    /**
     * {@code redact} scans formula cells' cached results, and when any cell is redacted clears the cached results
     * and sets the workbook to recalculate on open; {@code keep} leaves formula cells as they are.
     */
    public RedactOrKeep getFormulaCaches() {
        return formulaCaches;
    }

    public Excel setFormulaCaches(final RedactOrKeep formulaCaches) {
        this.formulaCaches = formulaCaches;
        return this;
    }

    /** {@code redact} redacts pivot caches and sets a redacted cache to refresh on open; {@code keep} leaves them. */
    public RedactOrKeep getPivotCaches() {
        return pivotCaches;
    }

    public Excel setPivotCaches(final RedactOrKeep pivotCaches) {
        this.pivotCaches = pivotCaches;
        return this;
    }

    /** {@code remove} deletes embedded objects that cannot be read; {@code keep} keeps and reports them. */
    public RemoveOrKeep getUninspectableObjects() {
        return uninspectableObjects;
    }

    public Excel setUninspectableObjects(final RemoveOrKeep uninspectableObjects) {
        this.uninspectableObjects = uninspectableObjects;
        return this;
    }

    /** Clears title, subject, keywords, categories, the comments field, and custom properties. */
    public boolean isRemoveDocumentProperties() {
        return removeDocumentProperties;
    }

    public Excel setRemoveDocumentProperties(final boolean removeDocumentProperties) {
        this.removeDocumentProperties = removeDocumentProperties;
        return this;
    }

    /** A copy of these settings. */
    public Excel copy() {
        return new Excel().setPrintHeadersFooters(printHeadersFooters).setCharts(charts).setFormulaCaches(formulaCaches)
                .setPivotCaches(pivotCaches).setUninspectableObjects(uninspectableObjects)
                .setRemoveDocumentProperties(removeDocumentProperties);
    }

    /**
     * Reads {@code excel} strictly, as the PhiSQL compilers check it: unknown keys, a value outside its list, and
     * a {@code removeDocumentProperties} that is not {@code true} or {@code false} are refused. Values are
     * case-sensitive.
     */
    public static class Adapter implements JsonDeserializer<Excel>, JsonSerializer<Excel> {

        @Override
        public Excel deserialize(final JsonElement json, final Type typeOfT,
                                 final JsonDeserializationContext context) throws JsonParseException {

            if (json == null || json.isJsonNull()) {
                return null;
            }

            final Excel excel = new Excel();

            for (final Map.Entry<String, JsonElement> entry : OfficeSetting.object(json, "excel").entrySet()) {
                final String key = entry.getKey();
                final JsonElement value = entry.getValue();
                switch (key) {
                    case "printHeadersFooters" -> excel.setPrintHeadersFooters(OfficeSetting.of(RedactOrKeep.class, value, "excel", key));
                    case "charts" -> excel.setCharts(OfficeSetting.of(RedactOrKeep.class, value, "excel", key));
                    case "formulaCaches" -> excel.setFormulaCaches(OfficeSetting.of(RedactOrKeep.class, value, "excel", key));
                    case "pivotCaches" -> excel.setPivotCaches(OfficeSetting.of(RedactOrKeep.class, value, "excel", key));
                    case "uninspectableObjects" -> excel.setUninspectableObjects(OfficeSetting.of(RemoveOrKeep.class, value, "excel", key));
                    case "removeDocumentProperties" -> excel.setRemoveDocumentProperties(OfficeSetting.bool(value, "excel", key));
                    default -> throw new JsonParseException("The excel has an unknown key '" + key + "'. Valid keys are "
                            + "printHeadersFooters, charts, formulaCaches, pivotCaches, uninspectableObjects, and "
                            + "removeDocumentProperties.");
                }
            }

            return excel;

        }

        @Override
        public JsonElement serialize(final Excel excel, final Type typeOfSrc, final JsonSerializationContext context) {
            final JsonObject object = new JsonObject();
            object.addProperty("printHeadersFooters", excel.getPrintHeadersFooters().value());
            object.addProperty("charts", excel.getCharts().value());
            object.addProperty("formulaCaches", excel.getFormulaCaches().value());
            object.addProperty("pivotCaches", excel.getPivotCaches().value());
            object.addProperty("uninspectableObjects", excel.getUninspectableObjects().value());
            object.addProperty("removeDocumentProperties", excel.isRemoveDocumentProperties());
            return object;
        }

    }

}
