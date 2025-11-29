package ai.philterd.phileas.services;

import ai.philterd.phileas.model.filtering.ApplyResult;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.filters.ApplyService;

import java.util.List;

public class PhileasApplyService implements ApplyService {

    public PhileasApplyService() {

    }

    @Override
    public ApplyResult apply(final List<Span> spans, final byte[] input, final MimeType mimeType) {

        if(mimeType == MimeType.TEXT_PLAIN) {

            final String text = new String(input);
            final StringBuilder sb = new StringBuilder(text);

            for(final Span span : spans) {

                // Replace the text with the replacement.
                sb.delete(span.getCharacterStart(), span.getCharacterEnd());
                sb.insert(span.getCharacterStart(), span.getReplacement());

            }

            return new ApplyResult(sb.toString());

        } else if(mimeType == MimeType.APPLICATION_PDF) {

            // TODO: Apply the redactions to the PDF.
            return null;

        } else {

            throw new IllegalArgumentException("Unsupported mime type: " + mimeType);

        }

    }

}
