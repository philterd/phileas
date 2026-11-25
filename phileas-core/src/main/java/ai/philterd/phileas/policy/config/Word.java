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
 * The policy's {@code config.word}: how a Word document is redacted beyond its body text. Body text, footnotes,
 * and endnotes are always redacted by the policy's identifiers, and authorship is always cleared.
 */
public class Word {

    /** redact or keep. */
    public enum RedactOrKeep implements OfficeSetting {
        REDACT, KEEP
    }

    /** remove or keep. */
    public enum RemoveOrKeep implements OfficeSetting {
        REMOVE, KEEP
    }

    /** remove or redact. */
    public enum RemoveOrRedact implements OfficeSetting {
        REMOVE, REDACT
    }

    /** accept or redact. */
    public enum AcceptOrRedact implements OfficeSetting {
        ACCEPT, REDACT
    }

    @SerializedName("headersFooters")
    @Expose
    private RedactOrKeep headersFooters = RedactOrKeep.REDACT;

    @SerializedName("comments")
    @Expose
    private RemoveOrRedact comments = RemoveOrRedact.REMOVE;

    @SerializedName("trackedChanges")
    @Expose
    private AcceptOrRedact trackedChanges = AcceptOrRedact.ACCEPT;

    @SerializedName("hiddenText")
    @Expose
    private RemoveOrRedact hiddenText = RemoveOrRedact.REMOVE;

    @SerializedName("charts")
    @Expose
    private RedactOrKeep charts = RedactOrKeep.REDACT;

    @SerializedName("uninspectableObjects")
    @Expose
    private RemoveOrKeep uninspectableObjects = RemoveOrKeep.REMOVE;

    @SerializedName("removeDocumentProperties")
    @Expose
    private boolean removeDocumentProperties = true;

    @SerializedName("highlight")
    @Expose
    private boolean highlight = false;

    /** {@code redact} redacts running headers and footers; {@code keep} leaves them unscanned. */
    public RedactOrKeep getHeadersFooters() {
        return headersFooters;
    }

    public Word setHeadersFooters(final RedactOrKeep headersFooters) {
        this.headersFooters = headersFooters;
        return this;
    }

    /** {@code remove} deletes every comment with its author; {@code redact} redacts comment text. */
    public RemoveOrRedact getComments() {
        return comments;
    }

    public Word setComments(final RemoveOrRedact comments) {
        this.comments = comments;
        return this;
    }

    /** {@code accept} accepts every revision and removes the history; {@code redact} redacts revision text. */
    public AcceptOrRedact getTrackedChanges() {
        return trackedChanges;
    }

    public Word setTrackedChanges(final AcceptOrRedact trackedChanges) {
        this.trackedChanges = trackedChanges;
        return this;
    }

    /** {@code remove} deletes text marked hidden; {@code redact} redacts it like visible text. */
    public RemoveOrRedact getHiddenText() {
        return hiddenText;
    }

    public Word setHiddenText(final RemoveOrRedact hiddenText) {
        this.hiddenText = hiddenText;
        return this;
    }

    /** {@code redact} redacts chart text and cached values; {@code keep} leaves charts as they are. */
    public RedactOrKeep getCharts() {
        return charts;
    }

    public Word setCharts(final RedactOrKeep charts) {
        this.charts = charts;
        return this;
    }

    /** {@code remove} deletes embedded objects that cannot be read; {@code keep} keeps and reports them. */
    public RemoveOrKeep getUninspectableObjects() {
        return uninspectableObjects;
    }

    public Word setUninspectableObjects(final RemoveOrKeep uninspectableObjects) {
        this.uninspectableObjects = uninspectableObjects;
        return this;
    }

    /** Clears title, subject, keywords, categories, the comments field, and custom properties. */
    public boolean isRemoveDocumentProperties() {
        return removeDocumentProperties;
    }

    public Word setRemoveDocumentProperties(final boolean removeDocumentProperties) {
        this.removeDocumentProperties = removeDocumentProperties;
        return this;
    }

    /** Wraps each replacement in a highlight. */
    public boolean isHighlight() {
        return highlight;
    }

    public Word setHighlight(final boolean highlight) {
        this.highlight = highlight;
        return this;
    }

    /** A copy of these settings. */
    public Word copy() {
        return new Word().setHeadersFooters(headersFooters).setComments(comments).setTrackedChanges(trackedChanges)
                .setHiddenText(hiddenText).setCharts(charts).setUninspectableObjects(uninspectableObjects)
                .setRemoveDocumentProperties(removeDocumentProperties).setHighlight(highlight);
    }

    /**
     * Reads {@code word} strictly, as the PhiSQL compilers check it: unknown keys, a value outside its list, and a
     * boolean setting that is not {@code true} or {@code false} are refused. Values are case-sensitive.
     */
    public static class Adapter implements JsonDeserializer<Word>, JsonSerializer<Word> {

        @Override
        public Word deserialize(final JsonElement json, final Type typeOfT,
                                final JsonDeserializationContext context) throws JsonParseException {

            if (json == null || json.isJsonNull()) {
                return null;
            }

            final Word word = new Word();

            for (final Map.Entry<String, JsonElement> entry : OfficeSetting.object(json, "word").entrySet()) {
                final String key = entry.getKey();
                final JsonElement value = entry.getValue();
                switch (key) {
                    case "headersFooters" -> word.setHeadersFooters(OfficeSetting.of(RedactOrKeep.class, value, "word", key));
                    case "comments" -> word.setComments(OfficeSetting.of(RemoveOrRedact.class, value, "word", key));
                    case "trackedChanges" -> word.setTrackedChanges(OfficeSetting.of(AcceptOrRedact.class, value, "word", key));
                    case "hiddenText" -> word.setHiddenText(OfficeSetting.of(RemoveOrRedact.class, value, "word", key));
                    case "charts" -> word.setCharts(OfficeSetting.of(RedactOrKeep.class, value, "word", key));
                    case "uninspectableObjects" -> word.setUninspectableObjects(OfficeSetting.of(RemoveOrKeep.class, value, "word", key));
                    case "removeDocumentProperties" -> word.setRemoveDocumentProperties(OfficeSetting.bool(value, "word", key));
                    case "highlight" -> word.setHighlight(OfficeSetting.bool(value, "word", key));
                    default -> throw new JsonParseException("The word has an unknown key '" + key + "'. Valid keys are "
                            + "headersFooters, comments, trackedChanges, hiddenText, charts, uninspectableObjects, "
                            + "removeDocumentProperties, and highlight.");
                }
            }

            return word;

        }

        @Override
        public JsonElement serialize(final Word word, final Type typeOfSrc, final JsonSerializationContext context) {
            final JsonObject object = new JsonObject();
            object.addProperty("headersFooters", word.getHeadersFooters().value());
            object.addProperty("comments", word.getComments().value());
            object.addProperty("trackedChanges", word.getTrackedChanges().value());
            object.addProperty("hiddenText", word.getHiddenText().value());
            object.addProperty("charts", word.getCharts().value());
            object.addProperty("uninspectableObjects", word.getUninspectableObjects().value());
            object.addProperty("removeDocumentProperties", word.isRemoveDocumentProperties());
            object.addProperty("highlight", word.isHighlight());
            return object;
        }

    }

}
