package com.mtnfog.phileas.services.disambiguation;

import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.SpanDisambiguationCacheService;
import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public abstract class AbstractSpanDisambiguationService {

    private static final Logger LOGGER = LogManager.getLogger(AbstractSpanDisambiguationService.class);

    // Can this vector size be increased over time as the number of documents process grows?
    // No, because it factors into the hash function.
    // Changing the size would require starting all over because the values in it would
    // no longer be valid because the hash function would have changed.

    private static final String DEFAULT_VECTOR_SIZE = "32";
    private static List<String> DEFAULT_STOPWORDS_EN = Arrays.asList("a", "as", "able", "about", "above", "according", "accordingly", "across", "actually", "after", "afterwards", "again", "against", "aint", "all", "allow", "allows", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "appear", "appreciate", "appropriate", "are", "arent", "around", "as", "aside", "ask", "asking", "associated", "at", "available", "away", "awfully", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "both", "brief", "but", "by", "cmon", "cs", "came", "can", "cant", "cannot", "cant", "cause", "causes", "certain", "certainly", "changes", "clearly", "co", "com", "come", "comes", "concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "couldnt", "course", "currently", "definitely", "described", "despite", "did", "didnt", "different", "do", "does", "doesnt", "doing", "dont", "done", "down", "downwards", "during", "each", "edu", "eg", "eight", "either", "else", "elsewhere", "enough", "entirely", "especially", "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "far", "few", "ff", "fifth", "first", "five", "followed", "following", "follows", "for", "former", "formerly", "forth", "four", "from", "further", "furthermore", "get", "gets", "getting", "given", "gives", "go", "goes", "going", "gone", "got", "gotten", "greetings", "had", "hadnt", "happens", "hardly", "has", "hasnt", "have", "havent", "having", "he", "hes", "hello", "help", "hence", "her", "here", "heres", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "hi", "him", "himself", "his", "hither", "hopefully", "how", "howbeit", "however", "i", "id", "ill", "im", "ive", "ie", "if", "ignored", "immediate", "in", "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates", "inner", "insofar", "instead", "into", "inward", "is", "isnt", "it", "itd", "itll", "its", "its", "itself", "just", "keep", "keeps", "kept", "know", "knows", "known", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "lets", "like", "liked", "likely", "little", "look", "looking", "looks", "ltd", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile", "merely", "might", "more", "moreover", "most", "mostly", "much", "must", "my", "myself", "name", "namely", "nd", "near", "nearly", "necessary", "need", "needs", "neither", "never", "nevertheless", "new", "next", "nine", "no", "nobody", "non", "none", "noone", "nor", "normally", "not", "nothing", "novel", "now", "nowhere", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", "overall", "own", "particular", "particularly", "per", "perhaps", "placed", "please", "plus", "possible", "presumably", "probably", "provides", "que", "quite", "qv", "rather", "rd", "re", "really", "reasonably", "regarding", "regardless", "regards", "relatively", "respectively", "right", "said", "same", "saw", "say", "saying", "says", "second", "secondly", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seriously", "seven", "several", "shall", "she", "should", "shouldnt", "since", "six", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specified", "specify", "specifying", "still", "sub", "such", "sup", "sure", "ts", "take", "taken", "tell", "tends", "th", "than", "thank", "thanks", "thanx", "that", "thats", "thats", "the", "their", "theirs", "them", "themselves", "then", "thence", "there", "theres", "thereafter", "thereby", "therefore", "therein", "theres", "thereupon", "these", "they", "theyd", "theyll", "theyre", "theyve", "think", "third", "this", "thorough", "thoroughly", "those", "though", "through", "throughout", "thru", "thus", "to", "together", "too", "took", "toward", "towards", "tried", "tries", "truly", "try", "trying", "twice", "two", "un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "value", "various", "very", "via", "viz", "vs", "want", "wants", "was", "wasnt", "way", "we", "wed", "well", "were", "weve", "welcome", "well", "went", "were", "werent", "what", "whats", "whatever", "when", "whence", "whenever", "where", "wheres", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whos", "whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with", "within", "without", "wont", "wonder", "would", "would", "wouldnt", "yes", "yet", "you", "youd", "youll", "youre", "youve", "your", "yours", "yourself", "yourselves");

    protected boolean enabled;
    protected final int vectorSize;
    protected final boolean ignoreStopWords;
    protected SpanDisambiguationCacheService spanDisambiguationCacheService;

    private Set<String> stopwords;

    public AbstractSpanDisambiguationService(final Properties applicationProperties) {

        this.vectorSize = Integer.valueOf(applicationProperties.getProperty("span.disambiguation.vector.size", DEFAULT_VECTOR_SIZE));
        this.ignoreStopWords = StringUtils.equalsIgnoreCase(applicationProperties.getProperty("span.disambiguation.ignore.stopwords", "true"), "true");
        this.stopwords = new HashSet<>(Arrays.asList(applicationProperties.getProperty("span.disambiguation.stopwords", StringUtils.join(DEFAULT_STOPWORDS_EN, ","))));

        final String useRedis = applicationProperties.getProperty("cache.redis.enabled", "false");

        if(StringUtils.equalsIgnoreCase(useRedis, "true")) {
            LOGGER.info("Using Redis disambiguation cache.");
            this.spanDisambiguationCacheService = new SpanDisambiguationRedisCacheService(applicationProperties);
        } else {
            LOGGER.info("Using local in-memory disambiguation cache.");
            this.spanDisambiguationCacheService = new SpanDisambiguationLocalCacheService();
        }

        this.enabled = StringUtils.equalsIgnoreCase(applicationProperties.getProperty("span.disambiguation.enabled", "false"), "true");

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
