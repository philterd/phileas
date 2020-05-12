package com.mtnfog.phileas.services.disambiguation;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.SpanDisambiguationCacheService;
import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractSpanDisambiguationService {

    private static final Logger LOGGER = LogManager.getLogger(AbstractSpanDisambiguationService.class);

    // Can this vector size be increased over time as the number of documents process grows?
    // No, because it factors into the hash function.
    // Changing the size would require starting all over because the values in it would
    // no longer be valid because the hash function would have changed.

    protected boolean enabled;
    protected final int vectorSize;
    protected final boolean ignoreStopWords;
    protected SpanDisambiguationCacheService spanDisambiguationCacheService;

    private Set<String> stopwords;

    public AbstractSpanDisambiguationService(final PhileasConfiguration phileasConfiguration) throws IOException {

        this.vectorSize = phileasConfiguration.spanDisambiguationVectorSize();
        this.ignoreStopWords = phileasConfiguration.spanDisambiguationIgnoreStopWords();
        this.stopwords = new HashSet<>(Arrays.asList(phileasConfiguration.spanDisambiguationStopWords().split("")));

        final boolean useRedis = phileasConfiguration.cacheRedisEnabled();

        if(useRedis) {
            LOGGER.info("Using Redis disambiguation cache.");
            this.spanDisambiguationCacheService = new SpanDisambiguationRedisCacheService(phileasConfiguration);
        } else {
            LOGGER.info("Using local in-memory disambiguation cache.");
            this.spanDisambiguationCacheService = new SpanDisambiguationLocalCacheService();
        }

        this.enabled = phileasConfiguration.spanDisambiguationEnabled();

    }

    protected double[] hash(Span span) {

        final double[] vector = new double[vectorSize];

        final String[] window = span.getWindow();

        for(final String token : window) {

            // Lowercase the token and remove any whitespace.
            final String lowerCasedToken = token.toLowerCase().trim();

            // Ignore stop words?
            if(ignoreStopWords && stopwords.contains(lowerCasedToken)) {

                // Ignore it as a stop word.

            } else {

                final int hash = Math.abs(MurmurHash3.hash32x86(token.getBytes()) % vectorSize);

                // We're only looking for what the window has. How many of each token is irrelevant.
                // TODO: But is it irrelevant though? If a word occurs more often than others
                // it is probably more indicative of the type than a word that only occurs once.
                vector[hash] = 1;

            }

        }

        return vector;

    }

    // TODO: I don't like this. I did this because the SpanDisambiguationService has to be created
    // before a boolean check to determine if the service is actually enabled. Making an
    // implementation of SpanDisambiguationService that does nothing seemed like a really
    // bad idea so I went this route instead. It needs worked on from service instantiation
    // up to service use.
    public boolean isEnabled() {
        return enabled;
    }

}
