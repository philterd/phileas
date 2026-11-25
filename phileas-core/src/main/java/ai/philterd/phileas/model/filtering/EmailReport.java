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
package ai.philterd.phileas.model.filtering;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;

/**
 * What the redaction of an email message left uninspected or took out beyond the policy's text redaction, so a
 * caller can warn about it: attachments and inline images kept without being inspected, attachments removed
 * because they could not be redacted, and signatures dropped because redacting invalidates them.
 */
public final class EmailReport {

    /** Why an attachment was removed under {@code attachments: redact}. */
    public enum RemovalReason {

        /** The attachment is not a type the redaction supports (PDF, Word, Excel). */
        @SerializedName("unsupportedType")
        UNSUPPORTED_TYPE,

        /** The attachment is a supported type but could not be redacted, such as a damaged file. */
        @SerializedName("redactionFailed")
        REDACTION_FAILED

    }

    /** One part of a message named in the report. */
    public static final class Part {

        private final String fileName;
        private final String contentType;
        private final int messageNumber;
        private final RemovalReason reason;

        /**
         * Creates a part.
         * @param fileName The part's filename, or {@code null} when it has none.
         * @param contentType The part's media type, such as {@code application/pdf}.
         * @param messageNumber The message the part is in: 0 for the message itself, otherwise the nested message's
         *                      number, counting from 1 in document order.
         * @param reason Why the part was removed, or {@code null} when it was not.
         */
        public Part(final String fileName, final String contentType, final int messageNumber, final RemovalReason reason) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.messageNumber = messageNumber;
            this.reason = reason;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public int getMessageNumber() {
            return messageNumber;
        }

        public RemovalReason getReason() {
            return reason;
        }

        @Override
        public boolean equals(final Object o) {
            return EqualsBuilder.reflectionEquals(this, o);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public String toString() {
            return (fileName == null ? "(no name)" : fileName) + " (" + contentType + ", message " + messageNumber + ")";
        }

    }

    private final List<Part> uninspectedAttachments;
    private final List<Part> uninspectedInlineImages;
    private final List<Part> removedAttachments;
    private final List<Part> droppedSignatures;

    public EmailReport(final List<Part> uninspectedAttachments, final List<Part> uninspectedInlineImages,
                       final List<Part> removedAttachments, final List<Part> droppedSignatures) {
        this.uninspectedAttachments = List.copyOf(uninspectedAttachments);
        this.uninspectedInlineImages = List.copyOf(uninspectedInlineImages);
        this.removedAttachments = List.copyOf(removedAttachments);
        this.droppedSignatures = List.copyOf(droppedSignatures);
    }

    /**
     * Attachments kept as they were, under {@code attachments: keep}. Their content was not inspected.
     * @return The attachments.
     */
    public List<Part> getUninspectedAttachments() {
        return uninspectedAttachments;
    }

    /**
     * Inline images kept as they were, under {@code inlineImages: keep}. Their content was not inspected.
     * @return The images.
     */
    public List<Part> getUninspectedInlineImages() {
        return uninspectedInlineImages;
    }

    /**
     * Attachments removed under {@code attachments: redact} because they could not be redacted.
     * @return The attachments, each with its {@link RemovalReason}.
     */
    public List<Part> getRemovedAttachments() {
        return removedAttachments;
    }

    /**
     * Signature parts (S/MIME or PGP) dropped because redacting the message invalidates them.
     * @return The signatures.
     */
    public List<Part> getDroppedSignatures() {
        return droppedSignatures;
    }

    /**
     * Whether anything was left uninspected or taken out beyond the policy's text redaction.
     * @return <code>true</code> if any list is not empty.
     */
    public boolean hasNotices() {
        return !uninspectedAttachments.isEmpty() || !uninspectedInlineImages.isEmpty()
                || !removedAttachments.isEmpty() || !droppedSignatures.isEmpty();
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

}
