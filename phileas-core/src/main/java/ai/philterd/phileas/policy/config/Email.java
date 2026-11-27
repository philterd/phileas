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
 * The policy's {@code config.email}: how an email message is redacted beyond its text. Subject, From, To, Cc,
 * and every text body part are always redacted by the policy's identifiers; these settings decide which other
 * headers are removed, and what happens to attachments and inline images.
 */
public class Email {

    /** What happens to attachments. */
    public enum Attachments {

        /** Attachments are left as they are, uninspected, and reported. */
        KEEP("keep"),

        /** Attachments are deleted, including their filenames. */
        REMOVE("remove"),

        /**
         * PDF, Word, Excel, RTF, CSV, and plain-text attachments are redacted with the policy; any other attachment is
         * removed and reported.
         */
        REDACT("redact");

        private final String value;

        Attachments(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        static Attachments of(final String value) {
            for (final Attachments attachments : values()) {
                if (attachments.value.equals(value)) {
                    return attachments;
                }
            }
            throw new JsonParseException("The email attachments '" + value
                    + "' is not valid. Valid values are keep, remove, and redact.");
        }

    }

    /** What happens to images embedded in the body and referenced by {@code cid:}. */
    public enum InlineImages {

        /** Inline images are left as they are, uninspected, and reported. */
        KEEP("keep"),

        /** Inline images are deleted and the references to them neutralized. */
        REMOVE("remove");

        private final String value;

        InlineImages(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        static InlineImages of(final String value) {
            for (final InlineImages inlineImages : values()) {
                if (inlineImages.value.equals(value)) {
                    return inlineImages;
                }
            }
            throw new JsonParseException("The email inlineImages '" + value
                    + "' is not valid. Valid values are keep and remove.");
        }

    }

    /** The header fields removed outright. */
    public static class Headers {

        @SerializedName("removeTechnical")
        @Expose
        private boolean removeTechnical = true;

        @SerializedName("removeIdentity")
        @Expose
        private boolean removeIdentity = true;

        @SerializedName("removeDate")
        @Expose
        private boolean removeDate = false;

        @SerializedName("removeSubject")
        @Expose
        private boolean removeSubject = false;

        /**
         * Removes {@code Received}, {@code Return-Path}, {@code Message-ID}, {@code User-Agent}, {@code X-Mailer},
         * {@code X-MimeOLE}, {@code DKIM-Signature}, {@code Authentication-Results}, {@code Received-SPF},
         * {@code Autocrypt}, and every header beginning {@code X-} or {@code ARC-}.
         * @return Whether the technical headers are removed.
         */
        public boolean isRemoveTechnical() {
            return removeTechnical;
        }

        public void setRemoveTechnical(final boolean removeTechnical) {
            this.removeTechnical = removeTechnical;
        }

        /**
         * Removes {@code Bcc}, {@code Reply-To}, {@code Sender}, and every header beginning {@code Resent-}.
         * @return Whether the identity headers are removed.
         */
        public boolean isRemoveIdentity() {
            return removeIdentity;
        }

        public void setRemoveIdentity(final boolean removeIdentity) {
            this.removeIdentity = removeIdentity;
        }

        public boolean isRemoveDate() {
            return removeDate;
        }

        public void setRemoveDate(final boolean removeDate) {
            this.removeDate = removeDate;
        }

        public boolean isRemoveSubject() {
            return removeSubject;
        }

        public void setRemoveSubject(final boolean removeSubject) {
            this.removeSubject = removeSubject;
        }

    }

    @SerializedName("headers")
    @Expose
    private Headers headers = new Headers();

    @SerializedName("attachments")
    @Expose
    private Attachments attachments = Attachments.KEEP;

    @SerializedName("inlineImages")
    @Expose
    private InlineImages inlineImages = InlineImages.KEEP;

    public Headers getHeaders() {
        return headers == null ? new Headers() : headers;
    }

    public void setHeaders(final Headers headers) {
        this.headers = headers;
    }

    public Attachments getAttachments() {
        return attachments == null ? Attachments.KEEP : attachments;
    }

    public void setAttachments(final Attachments attachments) {
        this.attachments = attachments;
    }

    public InlineImages getInlineImages() {
        return inlineImages == null ? InlineImages.KEEP : inlineImages;
    }

    public void setInlineImages(final InlineImages inlineImages) {
        this.inlineImages = inlineImages;
    }

    /**
     * Reads {@code email} strictly, so a policy that is not what the schema allows fails to load rather than
     * redacting email in a way the policy did not ask for: unknown keys, a header setting that is not a boolean,
     * and an {@code attachments} or {@code inlineImages} value outside its list are refused. Values are
     * case-sensitive.
     */
    public static class Adapter implements JsonDeserializer<Email>, JsonSerializer<Email> {

        @Override
        public Email deserialize(final JsonElement json, final Type typeOfT,
                                 final JsonDeserializationContext context) throws JsonParseException {

            if (json == null || json.isJsonNull()) {
                return null;
            }

            final Email email = new Email();

            for (final Map.Entry<String, JsonElement> entry : object(json, "email").entrySet()) {

                final JsonElement value = entry.getValue();

                switch (entry.getKey()) {
                    case "headers" -> email.setHeaders(headers(value));
                    case "attachments" -> email.setAttachments(Attachments.of(string(value, "attachments")));
                    case "inlineImages" -> email.setInlineImages(InlineImages.of(string(value, "inlineImages")));
                    default -> throw new JsonParseException("The email has an unknown key '" + entry.getKey()
                            + "'. Valid keys are headers, attachments, and inlineImages.");
                }

            }

            return email;

        }

        private static Headers headers(final JsonElement json) {

            final Headers headers = new Headers();

            for (final Map.Entry<String, JsonElement> entry : object(json, "email headers").entrySet()) {

                final boolean value = bool(entry.getValue(), entry.getKey());

                switch (entry.getKey()) {
                    case "removeTechnical" -> headers.setRemoveTechnical(value);
                    case "removeIdentity" -> headers.setRemoveIdentity(value);
                    case "removeDate" -> headers.setRemoveDate(value);
                    case "removeSubject" -> headers.setRemoveSubject(value);
                    default -> throw new JsonParseException("The email headers has an unknown key '" + entry.getKey()
                            + "'. Valid keys are removeTechnical, removeIdentity, removeDate, and removeSubject.");
                }

            }

            return headers;

        }

        private static JsonObject object(final JsonElement json, final String name) {
            if (!json.isJsonObject()) {
                throw new JsonParseException("The " + name + " must be an object.");
            }
            return json.getAsJsonObject();
        }

        private static String string(final JsonElement json, final String name) {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("The email " + name + " must be a string.");
            }
            return json.getAsString();
        }

        private static boolean bool(final JsonElement json, final String name) {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isBoolean()) {
                throw new JsonParseException("The email headers " + name + " must be true or false.");
            }
            return json.getAsBoolean();
        }

        @Override
        public JsonElement serialize(final Email email, final Type typeOfSrc, final JsonSerializationContext context) {

            final JsonObject headers = new JsonObject();
            headers.addProperty("removeTechnical", email.getHeaders().isRemoveTechnical());
            headers.addProperty("removeIdentity", email.getHeaders().isRemoveIdentity());
            headers.addProperty("removeDate", email.getHeaders().isRemoveDate());
            headers.addProperty("removeSubject", email.getHeaders().isRemoveSubject());

            final JsonObject object = new JsonObject();
            object.add("headers", headers);
            object.addProperty("attachments", email.getAttachments().getValue());
            object.addProperty("inlineImages", email.getInlineImages().getValue());
            return object;

        }

    }

}
