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
package ai.philterd.phileas.services.email;

import jakarta.activation.DataHandler;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Messages and attachments generated for the tests. None is taken from real correspondence. */
public final class EmailFixtures {

    public static final Session SESSION = Session.getInstance(new Properties());

    private EmailFixtures() {
    }

    /** Parses an .eml. */
    public static MimeMessage parse(final byte[] eml) throws Exception {
        return new MimeMessage(SESSION, new ByteArrayInputStream(eml));
    }

    public static byte[] bytes(final MimeMessage message) throws Exception {
        message.saveChanges();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        return out.toByteArray();
    }

    public static MimeBodyPart text(final String text, final String subtype) throws Exception {
        final MimeBodyPart part = new MimeBodyPart();
        part.setText(text, "UTF-8", subtype);
        return part;
    }

    public static MimeBodyPart attachment(final String fileName, final String type, final byte[] content) throws Exception {
        final MimeBodyPart part = new MimeBodyPart();
        part.setDataHandler(new DataHandler(new ByteArrayDataSource(content, type)));
        part.setFileName(fileName);
        part.setDisposition(MimeBodyPart.ATTACHMENT);
        return part;
    }

    public static MimeBodyPart multipart(final String subtype, final MimeBodyPart... parts) throws Exception {
        final MimeMultipart multipart = new MimeMultipart(subtype);
        for (final MimeBodyPart part : parts) {
            multipart.addBodyPart(part);
        }
        final MimeBodyPart wrapper = new MimeBodyPart();
        wrapper.setContent(multipart);
        return wrapper;
    }

    public static MimeBodyPart nested(final MimeMessage message) throws Exception {
        final MimeBodyPart part = new MimeBodyPart();
        part.setContent(parse(bytes(message)), "message/rfc822");
        return part;
    }

    /** Every header and every piece of text in a message and the messages nested in it, decoded. */
    public static String allText(final jakarta.mail.Part part) throws Exception {
        final StringBuilder text = new StringBuilder();
        final var headers = part.getAllHeaders();
        while (headers.hasMoreElements()) {
            final jakarta.mail.Header header = headers.nextElement();
            text.append(header.getName()).append(": ")
                    .append(jakarta.mail.internet.MimeUtility.decodeText(jakarta.mail.internet.MimeUtility.unfold(header.getValue())))
                    .append('\n');
        }
        final Object content = part.getContent();
        if (content instanceof jakarta.mail.Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                text.append(allText(multipart.getBodyPart(i)));
            }
        } else if (content instanceof jakarta.mail.Message message) {
            text.append(allText(message));
        } else if (content instanceof String string) {
            text.append(string).append('\n');
        } else if (part.isMimeType("message/*") || part.isMimeType("text/*")) {
            text.append(new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).append('\n');
        }
        return text.toString();
    }

    // --- Documents -----------------------------------------------------------------------------

    public static byte[] pdf(final String line) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 700);
                content.showText(line);
                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static final String RELS_NS = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String OFFICE_DOCUMENT = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument";

    public static byte[] docx(final String paragraph) throws IOException {
        return zip(Map.of(
                "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                        + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                        + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                        + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>",
                "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"" + RELS_NS + "\">"
                        + "<Relationship Id=\"rId1\" Type=\"" + OFFICE_DOCUMENT + "\" Target=\"word/document.xml\"/></Relationships>",
                "word/document.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                        + "<w:body><w:p><w:r><w:t xml:space=\"preserve\">" + paragraph + "</w:t></w:r></w:p></w:body></w:document>"));
    }

    public static byte[] xlsx(final String cell) throws IOException {
        return zip(Map.of(
                "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                        + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                        + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                        + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                        + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>",
                "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"" + RELS_NS + "\">"
                        + "<Relationship Id=\"rId1\" Type=\"" + OFFICE_DOCUMENT + "\" Target=\"xl/workbook.xml\"/></Relationships>",
                "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                        + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>",
                "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"" + RELS_NS + "\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>",
                "xl/worksheets/sheet1.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                        + "<sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>" + cell + "</t></is></c></row></sheetData></worksheet>"));
    }

    /** The text of every XML entry in a zip package. */
    public static String packageXml(final byte[] pkg) throws IOException {
        final StringBuilder xml = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pkg))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith(".xml")) {
                    xml.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        return xml.toString();
    }

    private static byte[] zip(final Map<String, String> entries) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (final Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    // --- Outlook .msg --------------------------------------------------------------------------

    /** A recipient of an Outlook message. Type 1 is To, 2 Cc, 3 Bcc. */
    public record Recipient(String name, String email, int type) {
    }

    /** An attachment of an Outlook message: a file, or, when {@code embedded} is set, another Outlook message. */
    public record MsgAttachment(String name, String mimeType, byte[] data, MsgContent embedded) {
    }

    /** The content of an Outlook message. */
    public record MsgContent(String subject, String fromName, String fromEmail, List<Recipient> recipients,
                             String textBody, String rtfBody, boolean withRecipientTypes, List<MsgAttachment> attachments) {
    }

    /**
     * An Outlook message written with POI's OLE2 file system, holding the chunks Outlook uses for these fields.
     * @param withRecipientTypes Whether each recipient carries its MAPI recipient type; without it, the display
     *                           lists say which a recipient is.
     */
    public static byte[] msg(final String subject, final String fromName, final String fromEmail,
                             final List<Recipient> recipients, final String textBody, final String rtfBody,
                             final boolean withRecipientTypes) throws IOException {
        return msg(new MsgContent(subject, fromName, fromEmail, recipients, textBody, rtfBody, withRecipientTypes, List.of()));
    }

    public static byte[] msg(final MsgContent content) throws IOException {
        try (POIFSFileSystem fs = new POIFSFileSystem(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            write(fs.getRoot(), content, 32);
            fs.writeFilesystem(out);
            return out.toByteArray();
        }
    }

    // The properties stream header is 32 bytes for a message and 24 for one embedded in another.
    private static void write(final DirectoryEntry root, final MsgContent content, final int headerSize) throws IOException {

        final List<Recipient> recipients = content.recipients();

        string(root, 0x001A, "IPM.Note");
        string(root, 0x0037, content.subject());
        string(root, 0x0C1A, content.fromName());
        string(root, 0x0C1F, content.fromEmail());
        string(root, 0x0E04, names(recipients, 1));
        string(root, 0x0E03, names(recipients, 2));
        string(root, 0x0E02, names(recipients, 3));
        if (content.textBody() != null) {
            string(root, 0x1000, content.textBody());
        }
        if (content.rtfBody() != null) {
            root.createDocument("__substg1.0_10090102", new ByteArrayInputStream(uncompressedRtf(content.rtfBody())));
        }
        root.createDocument("__properties_version1.0", new ByteArrayInputStream(new byte[headerSize]));

        for (int i = 0; i < recipients.size(); i++) {
            final Recipient recipient = recipients.get(i);
            final DirectoryEntry dir = root.createDirectory(String.format("__recip_version1.0_#%08X", i));
            string(dir, 0x3001, recipient.name());
            string(dir, 0x3003, recipient.email());
            string(dir, 0x39FE, recipient.email());
            final ByteBuffer properties = ByteBuffer.allocate(content.withRecipientTypes() ? 8 + 16 : 8).order(ByteOrder.LITTLE_ENDIAN);
            properties.put(new byte[8]);
            if (content.withRecipientTypes()) {
                // PR_RECIPIENT_TYPE (0x0C15), PT_LONG (0x0003).
                properties.putShort((short) 0x0003).putShort((short) 0x0C15).putInt(6).putInt(recipient.type()).putInt(0);
            }
            dir.createDocument("__properties_version1.0", new ByteArrayInputStream(properties.array()));
        }

        for (int i = 0; i < content.attachments().size(); i++) {
            final MsgAttachment attachment = content.attachments().get(i);
            final DirectoryEntry dir = root.createDirectory(String.format("__attach_version1.0_#%08X", i));
            string(dir, 0x3707, attachment.name());
            string(dir, 0x3001, attachment.name());
            if (attachment.embedded() != null) {
                // PR_ATTACH_DATA_OBJ as a directory holding the embedded message.
                write(dir.createDirectory("__substg1.0_3701000D"), attachment.embedded(), 24);
            } else {
                string(dir, 0x370E, attachment.mimeType());
                dir.createDocument("__substg1.0_37010102", new ByteArrayInputStream(attachment.data()));
            }
            dir.createDocument("__properties_version1.0", new ByteArrayInputStream(new byte[8]));
        }

    }

    private static String names(final List<Recipient> recipients, final int type) {
        return String.join("; ", recipients.stream().filter(r -> r.type() == type).map(Recipient::name).toList());
    }

    private static void string(final DirectoryEntry dir, final int property, final String value) throws IOException {
        if (value != null && !value.isEmpty()) {
            dir.createDocument(String.format("__substg1.0_%04X001F", property),
                    new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_16LE)));
        }
    }

    // PR_RTF_COMPRESSED in its uncompressed ("MELA") form.
    private static byte[] uncompressedRtf(final String rtf) {
        final byte[] data = rtf.getBytes(StandardCharsets.US_ASCII);
        final ByteBuffer buffer = ByteBuffer.allocate(16 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(12 + data.length).putInt(data.length).put("MELA".getBytes(StandardCharsets.US_ASCII)).putInt(0).put(data);
        return buffer.array();
    }

}
