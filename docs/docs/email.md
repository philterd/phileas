# Email Redaction

The `phileas-email` module redacts email messages with a policy: MIME messages (`.eml`, `message/rfc822`) and Outlook messages (`.msg`, `application/vnd.ms-outlook`). Both are written out as `.eml`. An Outlook message cannot be written back in its own format, so it is converted.

Detection is the same as for text, using the policy's filters, strategies, and context. Like all detection, it is probabilistic and depends on the policy; validate the output against your own messages.

## Adding the module

`phileas-email` is not included in `phileas`. Add it alongside `phileas` or `phileas-core`, with the same version:

```xml
<dependency>
    <groupId>ai.philterd</groupId>
    <artifactId>phileas-email</artifactId>
    <version>4.6.0</version>
</dependency>
```

It brings in `phileas-pdf` and `phileas-ooxml`, which redact attachments, Jakarta Mail (Eclipse Angus) for MIME, and Apache POI's HSMF for Outlook messages.

## Redacting a message

`EmailFilterService` takes the message's bytes and returns the redacted `.eml`:

```java
final PhileasConfiguration configuration = new PhileasConfiguration(new Properties());
final EmailFilterService service = new EmailFilterService(configuration,
        new DefaultContextService(), new InMemoryVectorService(), null);

final Policy policy = new Gson().fromJson("{\"identifiers\":{\"emailAddress\":{},\"ssn\":{}}}", Policy.class);

final byte[] input = Files.readAllBytes(Path.of("message.msg"));
final BinaryDocumentFilterResult result = service.filter(policy, "my-context", input, MimeType.MESSAGE_RFC822);

Files.write(Path.of("redacted.eml"), result.getDocument());
```

The input format is read from the message itself: an Outlook message is recognized by its file signature, and anything else is read as MIME. The output MIME type may be `null` or `MimeType.MESSAGE_RFC822`. Input that is not an email message, because it has no standard message header (such as `From`, `Subject`, `Date`, or `Content-Type`) or has a line in its header block that is not a header, is rejected with an `IllegalArgumentException`, as is a message whose parts and nested messages are more than 100 levels deep.

As with the other services, `EmailFilterService` can be created without context and vector services and given them on each call, with `filter(policy, contextService, vectorService, context, input, outputMimeType)`.

### What is redacted

The policy's identifiers always redact:

* the `Subject`, `From`, `To`, and `Cc` headers, and
* every text body part. Plain text is redacted as text. HTML is redacted as markup, so values in attributes such as `mailto:` links are covered, and also by its visible text, so a value split by tags or written with character references (`pat&#64;example.com`) is found. A value inside a `cid:` reference is left alone, since it names a part of the message.

Delivery status notifications (`message/delivery-status` and similar parts) are redacted as text. A message nested in another (`message/rfc822`), such as a forwarded message, is processed the same way, recursively, with the same settings.

Other headers are kept as they are unless the policy's `config.email` removes them. A header value encoded as an RFC 2047 encoded word is decoded before it is redacted.

### Outlook messages

From an Outlook message, the subject, sender, recipients (To, Cc, and Bcc), sent date, bodies, and attachments are carried over, and an attached Outlook message becomes a nested message. A message whose only body is RTF has its visible text read with [`RtfTextExtractor`](rtf.md#reading-text), so it is redacted, as plain text, rather than dropped; an RTF body that is malformed is refused with an `RtfFormatException`. An Outlook message's stored transport headers (`Received` and the like) are not carried over.

### Signed and encrypted messages

A signed message (S/MIME or PGP, `multipart/signed`) is redacted and its signature part is dropped, since redacting the message invalidates the signature. The dropped signature is reported.

An encrypted message (`application/pkcs7-mime`, `multipart/encrypted`, or a body holding a PGP message) cannot be read, so it is refused with an `EncryptedEmailException` and nothing is written. This applies to a nested message too. An opaque S/MIME signed message (`application/pkcs7-mime` with `smime-type=signed-data`) is refused the same way, since its content is inside the signature.

### Spans

A span's `characterStart` and `characterEnd` are offsets within its field, and `paragraphNumber` is the field's number. Fields are numbered in this order: each message's `Subject`, `From`, `To`, and `Cc` headers (each only if present), then its text body parts in document order, with a nested message's fields where the message is. Offsets in an HTML body are into its markup.

Spans in redacted attachments are not part of the message's explanation.

### Applying spans

`apply(policy, input, spans, outputMimeType)` applies a list of spans, for example ones a reviewer accepted or added, by their field number and offsets. Headers, attachments, and inline images are handled by the policy's `config.email`, and PDF, Word, and Excel attachments under `attachments: redact` are redacted with the policy.

## Email settings in the policy

The policy's `config.email` decides what happens beyond the text. The defaults match Philter Desktop's, so a policy without `config.email` redacts email as Philter Desktop does:

```json
{
   "config": {
      "email": {
         "headers": {
            "removeTechnical": true,
            "removeIdentity": true,
            "removeDate": false,
            "removeSubject": false
         },
         "attachments": "keep",
         "inlineImages": "keep"
      }
   }
}
```

| Setting | Default | Effect |
|---|---|---|
| `headers.removeTechnical` | `true` | Removes `Received`, `Return-Path`, `Message-ID`, `User-Agent`, `X-Mailer`, `X-MimeOLE`, `DKIM-Signature`, `Authentication-Results`, `Received-SPF`, `Autocrypt`, and every header beginning `X-` or `ARC-`. |
| `headers.removeIdentity` | `true` | Removes `Bcc`, `Reply-To`, `Sender`, and every header beginning `Resent-`. |
| `headers.removeDate` | `false` | Removes the `Date` header, whatever its format, without relying on a date identifier. |
| `headers.removeSubject` | `false` | Removes the `Subject` header instead of redacting it. |
| `attachments` | `keep` | `keep` leaves attachments as they are, uninspected, and reports them. `remove` deletes them, including their filenames. `redact` redacts each PDF, Word (`.docx`), and Excel (`.xlsx`) attachment with the same policy, and removes and reports any other attachment and any that cannot be redacted. |
| `inlineImages` | `keep` | `keep` leaves images embedded in the body as they are, uninspected, and reports them. `remove` deletes them and points each `cid:` reference to them at `about:blank`. Independent of `attachments`. |

Header names match without regard to case. A removed header is removed entirely, in the message and in every nested message, and is not scanned. A message without a `Date` or `Message-ID` is written without one; none is generated.

Under `attachments: remove`, a forwarded message attached as a file is removed too. Under `keep` and `redact`, it is redacted as a nested message. An attachment is recognized as PDF, Word, or Excel by its media type or filename extension, or, for `application/octet-stream` without an extension, by its content. PDF attachments are redacted as by `PdfFilterService`, with the policy's `config.pdf`, so a page with a redaction is flattened to an image.

A `config.email` that is not an object, has an unknown key, has a header setting that is not `true` or `false`, or has an `attachments` or `inlineImages` value outside its list is refused when the policy is loaded. Values are case-sensitive. In PhiSQL:

```
CONFIGURE EMAIL (
  headers = (removeTechnical = TRUE, removeIdentity = TRUE, removeDate = TRUE, removeSubject = TRUE),
  attachments = 'redact',
  inlineImages = 'remove'
);
```

## The email report

`BinaryDocumentFilterResult.getEmail()` returns an `EmailReport` for an email message (and `null` for other documents), so a caller can warn about content that was not checked:

* `getUninspectedAttachments()`: attachments kept under `attachments: keep`.
* `getUninspectedInlineImages()`: inline images kept under `inlineImages: keep`.
* `getRemovedAttachments()`: attachments removed under `attachments: redact` because they could not be redacted, each with a reason: `unsupportedType`, or `redactionFailed` for a supported type that could not be read, such as a damaged file.
* `getDroppedSignatures()`: signature parts dropped from signed messages.

Each entry has the part's `fileName` (or `null`), `contentType`, and `messageNumber`: 0 for the message itself, otherwise the nested message's number, counting from 1 in document order. When serialized to JSON, the report is `email`.

## Leftover check

The policy's [leftover check](filter_policies/filter_policies.md#leftover-check) covers the redacted fields and the redacted attachments, as part of the message's own result:

* A finding in a field has the field's number as `paragraphNumber` and offsets into the redacted field.
* A finding in a redacted attachment has the attachment's filename as `attachment` (`getAttachment()`), with its position as for a document of its type.
* A redacted attachment whose output has no readable text, such as a PDF whose pages were all flattened to images, or whose check failed, is listed in `uncheckedAttachments` (`getUncheckedAttachments()`). Nothing in it was checked.

Attachments and inline images that were kept are not checked; the email report lists them.
