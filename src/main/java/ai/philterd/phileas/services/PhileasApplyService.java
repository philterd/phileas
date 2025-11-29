package ai.philterd.phileas.services;

import ai.philterd.phileas.model.filtering.ApplyResult;
import ai.philterd.phileas.model.filtering.IncrementalRedaction;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.filters.ApplyService;
import ai.philterd.phileas.services.tokens.TokenCounter;
import ai.philterd.phileas.services.tokens.WhitespaceTokenCounter;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.List;

public class PhileasApplyService implements ApplyService {

    private final TokenCounter tokenCounter;

    public PhileasApplyService() {

        this.tokenCounter = new WhitespaceTokenCounter();

    }

    @Override
    public ApplyResult apply(final List<Span> spans, final String input) {

        final StringBuilder sb = new StringBuilder(input);
        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();
        final long tokens = tokenCounter.countTokens(input);

        for(final Span span : spans) {

            // Replace the text with the replacement.
            sb.delete(span.getCharacterStart(), span.getCharacterEnd());
            sb.insert(span.getCharacterStart(), span.getReplacement());

            // Generate the incremental redaction.
            final String hash = DigestUtils.sha256Hex(sb.toString());
            final IncrementalRedaction incrementalRedaction = new IncrementalRedaction(hash, span, sb.toString());
            incrementalRedactions.add(incrementalRedaction);

        }

        return new ApplyResult(sb.toString(), incrementalRedactions, tokens);

    }

    @Override
    public ApplyResult apply(final List<Span> spans, final byte[] input, final MimeType mimeType) {

//        final StringBuilder sb = new StringBuilder(input);
//        final List<IncrementalRedaction> incrementalRedactions = new ArrayList<>();
//        final long tokens = tokenCounter.countTokens(input);
//
//        for(final Span span : spans) {
//
//            // Replace the text with the replacement.
//            sb.delete(span.getCharacterStart(), span.getCharacterEnd());
//            sb.insert(span.getCharacterStart(), span.getReplacement());
//
//            // Generate the incremental redaction.
//            final String hash = DigestUtils.sha256Hex(sb.toString());
//            final IncrementalRedaction incrementalRedaction = new IncrementalRedaction(hash, span, sb.toString());
//            incrementalRedactions.add(incrementalRedaction);
//
//        }
//
//        return new ApplyResult(sb.toString(), incrementalRedactions, tokens);

        return null;

    }

}
