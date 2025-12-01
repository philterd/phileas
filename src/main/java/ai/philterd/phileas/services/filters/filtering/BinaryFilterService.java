package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.ContextService;

public abstract class BinaryFilterService extends FilterService<BinaryDocumentFilterResult> {

    /**
     * Filter text from a binary document.
     * @param policy The {@link Policy} to apply.
     * @param context The redaction context.
     * @param input The input document as a byte array.
     * @param mimeType The input {@link MimeType}.
     * @param outputMimeType The output {@link MimeType}.
     * @return A {@link BinaryDocumentFilterResult}.
     * @throws Exception Thrown if the text cannot be filtered.
     */
    public abstract BinaryDocumentFilterResult filter(final Policy policy, final String context, final byte[] input, final MimeType mimeType, final MimeType outputMimeType) throws Exception;

    public BinaryFilterService(final PhileasConfiguration phileasConfiguration,
                         final ContextService contextService) {

        super(phileasConfiguration, contextService);

    }

}
