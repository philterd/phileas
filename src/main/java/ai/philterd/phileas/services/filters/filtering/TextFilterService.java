package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.ContextService;

public abstract class TextFilterService extends FilterService<TextFilterResult> {

    /**
     * Filter text from plain text.
     * @param policy The {@link Policy} to apply.
     * @param context The redaction context.
     * @param input The input document as a byte array.
     * @return A {@link TextFilterResult}.
     * @throws Exception Thrown if the text cannot be filtered.
     */
    public abstract TextFilterResult filter(final Policy policy, final String context, final String input) throws Exception;

    public TextFilterService(final PhileasConfiguration phileasConfiguration,
                               final ContextService contextService) {

        super(phileasConfiguration, contextService);

    }

}
